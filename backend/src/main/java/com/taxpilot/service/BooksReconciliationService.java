package com.taxpilot.service;

import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import com.taxpilot.domain.audit.AuditAction;
import com.taxpilot.domain.tax.FilingPeriod;
import com.taxpilot.dto.BooksReconciliationCheck;
import com.taxpilot.dto.BooksReconciliationResponse;
import com.taxpilot.repository.BusinessMembershipRepository;
import com.taxpilot.repository.FilingPeriodRepository;

@Service
public class BooksReconciliationService {

    private static final long MAX_FILE_SIZE = 10L * 1024 * 1024;
    private static final int MAX_ROWS = 20_000;
    private static final BigDecimal TOLERANCE = new BigDecimal("0.01");
    private static final List<String> REQUIRED_HEADERS =
            List.of("transaction_date", "transaction_type", "amount", "vat_amount");

    private final FilingPeriodRepository filingPeriodRepository;
    private final BusinessMembershipRepository membershipRepository;
    private final AuditService auditService;

    public BooksReconciliationService(FilingPeriodRepository filingPeriodRepository,
                                     BusinessMembershipRepository membershipRepository,
                                     AuditService auditService) {
        this.filingPeriodRepository = filingPeriodRepository;
        this.membershipRepository = membershipRepository;
        this.auditService = auditService;
    }

    @Transactional
    public BooksReconciliationResponse reconcile(UUID userId, UUID businessId, UUID filingPeriodId,
                                                  MultipartFile file) {
        requireActiveMembership(userId, businessId);
        FilingPeriod period = filingPeriodRepository.findByIdAndBusinessId(filingPeriodId, businessId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Filing period not found"));
        if (file == null || file.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Choose a non-empty CSV file");
        }
        if (file.getSize() > MAX_FILE_SIZE) {
            throw new ResponseStatusException(HttpStatus.PAYLOAD_TOO_LARGE, "CSV file must be 10 MB or smaller");
        }
        String fileName = file.getOriginalFilename();
        if (fileName == null || !fileName.toLowerCase(Locale.ROOT).endsWith(".csv")) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Upload a .csv file");
        }

        String csv;
        try {
            byte[] content = file.getBytes();
            if (content.length > MAX_FILE_SIZE) {
                throw new ResponseStatusException(HttpStatus.PAYLOAD_TOO_LARGE, "CSV file must be 10 MB or smaller");
            }
            csv = new String(content, StandardCharsets.UTF_8);
        } catch (IOException exception) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "The CSV file could not be read", exception);
        }
        if (!csv.isEmpty() && csv.charAt(0) == '\uFEFF') {
            csv = csv.substring(1);
        }

        List<List<String>> rows = parseCsv(csv);
        if (rows.size() < 2) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "The CSV must include a header and at least one transaction");
        }
        if (rows.size() - 1 > MAX_ROWS) {
            throw new ResponseStatusException(HttpStatus.PAYLOAD_TOO_LARGE, "CSV files may contain at most 20,000 transactions");
        }

        Map<String, Integer> columns = headerColumns(rows.get(0));
        int dateColumn = columns.get("transaction_date");
        int typeColumn = columns.get("transaction_type");
        int amountColumn = columns.get("amount");
        int vatColumn = columns.get("vat_amount");

        BigDecimal bookSales = BigDecimal.ZERO;
        BigDecimal purchaseInputVat = BigDecimal.ZERO;
        int salesCount = 0;
        int purchaseCount = 0;
        for (int index = 1; index < rows.size(); index++) {
            List<String> row = rows.get(index);
            if (isBlankRow(row)) {
                continue;
            }
            int rowNumber = index + 1;
            if (row.size() != rows.get(0).size()) {
                throw invalidRow(rowNumber, "column count does not match the header");
            }

            LocalDate transactionDate;
            try {
                transactionDate = LocalDate.parse(row.get(dateColumn).trim());
            } catch (DateTimeParseException exception) {
                throw invalidRow(rowNumber, "transaction_date must use YYYY-MM-DD format");
            }
            if (transactionDate.isBefore(period.getPeriodStart()) || transactionDate.isAfter(period.getPeriodEnd())) {
                throw invalidRow(rowNumber, "transaction_date is outside the selected filing period");
            }

            String type = row.get(typeColumn).trim().toUpperCase(Locale.ROOT);
            if (!type.equals("SALE") && !type.equals("PURCHASE")) {
                throw invalidRow(rowNumber, "transaction_type must be SALE or PURCHASE");
            }
            BigDecimal amount = parseAmount(row.get(amountColumn), rowNumber, "amount");
            BigDecimal vat = parseAmount(row.get(vatColumn), rowNumber, "vat_amount");
            if (type.equals("SALE")) {
                bookSales = bookSales.add(amount);
                salesCount++;
            } else {
                purchaseInputVat = purchaseInputVat.add(vat);
                purchaseCount++;
            }
        }
        if (salesCount + purchaseCount == 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "The CSV contains no valid transactions");
        }

        bookSales = money(bookSales);
        purchaseInputVat = money(purchaseInputVat);
        List<BooksReconciliationCheck> checks = List.of(
                compare("BOOKS_VS_TAXABLE_SALES", "Book sales versus recorded taxable sales",
                        bookSales, period.getTaxableSales()),
                compare("BOOKS_VS_ETIMS_SALES", "Book sales versus recorded eTIMS sales",
                        bookSales, period.getEtimsDeclaredSales()),
                compare("PURCHASE_VAT_VS_INPUT_VAT", "Purchase VAT versus recorded input VAT",
                        purchaseInputVat, period.getInputVat())
        );
        boolean passed = checks.stream().noneMatch(check -> "MISMATCH".equals(check.status()));
        auditService.record(userId, null, AuditAction.BOOKS_RECONCILIATION_RUN, "FILING_PERIOD",
                period.getId(), businessId, com.taxpilot.domain.audit.AuditOutcome.SUCCESS,
                "Books reconciliation processed " + (salesCount + purchaseCount) + " transactions; "
                        + checks.stream().filter(check -> "MISMATCH".equals(check.status())).count() + " mismatch(es)");

        return new BooksReconciliationResponse(period.getId(), period.getPeriodStart(), period.getPeriodEnd(),
                fileName, salesCount + purchaseCount, salesCount, purchaseCount, bookSales,
                purchaseInputVat, passed, checks);
    }

    private Map<String, Integer> headerColumns(List<String> header) {
        Map<String, Integer> columns = new HashMap<>();
        for (int index = 0; index < header.size(); index++) {
            String name = header.get(index).trim().toLowerCase(Locale.ROOT);
            if (columns.putIfAbsent(name, index) != null) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "The CSV contains a duplicate column: " + name);
            }
        }
        for (String required : REQUIRED_HEADERS) {
            if (!columns.containsKey(required)) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "The CSV is missing required column: " + required);
            }
        }
        return columns;
    }

    private List<List<String>> parseCsv(String csv) {
        List<List<String>> rows = new ArrayList<>();
        List<String> row = new ArrayList<>();
        StringBuilder field = new StringBuilder();
        boolean quoted = false;
        boolean closedQuote = false;
        for (int index = 0; index < csv.length(); index++) {
            char character = csv.charAt(index);
            if (quoted) {
                if (character == '"') {
                    if (index + 1 < csv.length() && csv.charAt(index + 1) == '"') {
                        field.append('"');
                        index++;
                    } else {
                        quoted = false;
                        closedQuote = true;
                    }
                } else {
                    field.append(character);
                }
                continue;
            }
            if (character == ',' ) {
                row.add(field.toString());
                field.setLength(0);
                closedQuote = false;
            } else if (character == '\n' || character == '\r') {
                row.add(field.toString());
                field.setLength(0);
                if (!isBlankRow(row)) {
                    rows.add(List.copyOf(row));
                }
                row.clear();
                closedQuote = false;
                if (character == '\r' && index + 1 < csv.length() && csv.charAt(index + 1) == '\n') {
                    index++;
                }
            } else if (character == '"' && field.isEmpty() && !closedQuote) {
                quoted = true;
            } else {
                if (closedQuote || character == '"') {
                    throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "The CSV contains invalid quoting");
                }
                field.append(character);
            }
        }
        if (quoted) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "The CSV contains an unterminated quoted field");
        }
        if (!field.isEmpty() || !row.isEmpty() || closedQuote) {
            row.add(field.toString());
            if (!isBlankRow(row)) {
                rows.add(List.copyOf(row));
            }
        }
        return rows;
    }

    private boolean isBlankRow(List<String> row) {
        return row.stream().allMatch(String::isBlank);
    }

    private BigDecimal parseAmount(String value, int rowNumber, String column) {
        try {
            BigDecimal amount = new BigDecimal(value.trim());
            if (amount.signum() < 0) {
                throw invalidRow(rowNumber, column + " must be zero or greater");
            }
            if (amount.stripTrailingZeros().scale() > 2) {
                throw invalidRow(rowNumber, column + " must have no more than two decimal places");
            }
            return money(amount);
        } catch (NumberFormatException exception) {
            throw invalidRow(rowNumber, column + " must be a valid number without thousands separators");
        }
    }

    private BooksReconciliationCheck compare(String code, String description, BigDecimal booksAmount,
                                             BigDecimal recordedAmount) {
        if (recordedAmount == null) {
            return new BooksReconciliationCheck(code, booksAmount, null, null, "NOT_AVAILABLE",
                    description + " could not be checked because no filing value was recorded.");
        }
        BigDecimal difference = money(booksAmount.subtract(recordedAmount));
        boolean matched = difference.abs().compareTo(TOLERANCE) <= 0;
        return new BooksReconciliationCheck(code, booksAmount, money(recordedAmount), difference,
                matched ? "MATCH" : "MISMATCH",
                matched ? description + " match within KES 0.01."
                        : description + " differ by KES " + difference.abs().toPlainString() + ".");
    }

    private ResponseStatusException invalidRow(int rowNumber, String reason) {
        return new ResponseStatusException(HttpStatus.BAD_REQUEST, "CSV row " + rowNumber + ": " + reason);
    }

    private BigDecimal money(BigDecimal value) {
        return value.setScale(2, RoundingMode.HALF_UP);
    }

    private void requireActiveMembership(UUID userId, UUID businessId) {
        membershipRepository.findByBusinessIdAndUserId(businessId, userId)
                .filter(com.taxpilot.domain.business.BusinessMembership::isActive)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Business not found"));
    }
}
