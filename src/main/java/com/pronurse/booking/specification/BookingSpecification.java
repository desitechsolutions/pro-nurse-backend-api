package com.pronurse.booking.specification;

import com.pronurse.booking.dto.BookingHistoryFilterRequest;
import com.pronurse.booking.entity.Booking;
import com.pronurse.booking.entity.BookingItem;
import jakarta.persistence.criteria.*;
import org.springframework.data.jpa.domain.Specification;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

public class BookingSpecification {

    public static Specification<Booking> buildPatientSpec(String mobile, BookingHistoryFilterRequest filter) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            // User filter - patient
            predicates.add(cb.equal(root.get("patientUser").get("mobile"), mobile));

            // Apply common filters
            applyCommonFilters(root, query, cb, predicates, filter);

            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }

    public static Specification<Booking> buildNurseSpec(String mobile, BookingHistoryFilterRequest filter) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            // User filter - assigned nurse
            predicates.add(cb.equal(root.get("assignedNurseUser").get("mobile"), mobile));

            // Apply common filters
            applyCommonFilters(root, query, cb, predicates, filter);

            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }

    private static void applyCommonFilters(
            Root<Booking> root,
            CriteriaQuery<?> query,
            CriteriaBuilder cb,
            List<Predicate> predicates,
            BookingHistoryFilterRequest filter) {

        // Date range filter
        if (filter.getStartDate() != null) {
            predicates.add(cb.greaterThanOrEqualTo(
                    root.get("bookingDate"), filter.getStartDate()));
        }
        if (filter.getEndDate() != null) {
            predicates.add(cb.lessThanOrEqualTo(
                    root.get("bookingDate"), filter.getEndDate()));
        }

        // Status filter
        if (filter.getStatuses() != null && !filter.getStatuses().isEmpty()) {
            predicates.add(root.get("bookingStatus").in(filter.getStatuses()));
        }

        // Service IDs filter
        if (filter.getServiceIds() != null && !filter.getServiceIds().isEmpty()) {
            Join<Booking, BookingItem> items = root.join("selectedItems", JoinType.INNER);
            predicates.add(items.get("service").get("id").in(filter.getServiceIds()));
        }

        // Amount range filter
        if (filter.getMinAmount() != null || filter.getMaxAmount() != null) {
            Subquery<BigDecimal> sumQuery = query.subquery(BigDecimal.class);
            Root<BookingItem> itemRoot = sumQuery.from(BookingItem.class);
            sumQuery.select(cb.sum(itemRoot.get("priceCharged")))
                    .where(cb.equal(itemRoot.get("booking"), root));

            if (filter.getMinAmount() != null) {
                predicates.add(cb.greaterThanOrEqualTo(sumQuery, filter.getMinAmount()));
            }
            if (filter.getMaxAmount() != null) {
                predicates.add(cb.lessThanOrEqualTo(sumQuery, filter.getMaxAmount()));
            }
        }

        // Search text filter (booking number or nurse name)
        if (filter.getSearchText() != null && !filter.getSearchText().isEmpty()) {
            String searchPattern = "%" + filter.getSearchText().toLowerCase() + "%";
            
            Predicate bookingNoMatch = cb.like(
                    cb.lower(root.get("bookingNo")), searchPattern);
            
            Predicate nurseNameMatch = cb.like(
                    cb.lower(root.get("assignedNurseUser").get("name")), searchPattern);
            
            Predicate patientNameMatch = cb.like(
                    cb.lower(root.get("patientUser").get("name")), searchPattern);
            
            predicates.add(cb.or(bookingNoMatch, nurseNameMatch, patientNameMatch));
        }

        // Emergency only filter
        if (filter.getEmergencyOnly() != null && filter.getEmergencyOnly()) {
            predicates.add(cb.isTrue(root.get("isEmergency")));
        }
    }
}


