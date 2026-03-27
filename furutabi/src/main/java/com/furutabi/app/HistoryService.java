package com.furutabi.app;

import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

@Service
public class HistoryService {

    private static final DateTimeFormatter HISTORY_TIME = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    private final JdbcTemplate jdbcTemplate;

    public HistoryService(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public HistoryPageData loadHistory(String email) {
        UserRow currentUser = requireUser(email);
        List<HistoryItem> items = new ArrayList<>();
        items.addAll(loadHostedProposalItems(currentUser.id()));
        items.addAll(loadApplicantApplicationItems(currentUser.id()));
        items.addAll(loadChatItems(currentUser.id()));
        items.addAll(loadMapRecordItems(currentUser.id()));
        items.addAll(loadFootprintItems(currentUser.id()));
        items.sort(Comparator.comparing(HistoryItem::occurredAtValue).reversed().thenComparing(HistoryItem::historyKey));
        return new HistoryPageData(displayName(currentUser.nickname(), currentUser.name(), currentUser.email()), items);
    }

    private List<HistoryItem> loadHostedProposalItems(long currentUserId) {
        return jdbcTemplate.query(
            """
                SELECT p.id, p.proposal_type, p.title, p.updated_at
                FROM proposals p
                WHERE p.host_user_id = ?
                  AND p.status = 'published'
                  AND p.deleted_at IS NULL
                ORDER BY p.updated_at DESC, p.id DESC
                """,
            (rs, rowNum) -> new HistoryItem(
                "proposal-" + rs.getLong("id"),
                "Hosted proposal",
                rs.getString("proposal_type"),
                rs.getString("title"),
                "A published proposal you host.",
                historyTime(rs.getTimestamp("updated_at")),
                timestampOrMin(rs.getTimestamp("updated_at")),
                proposalDetailPath(rs.getString("proposal_type"), rs.getLong("id"))
            ),
            currentUserId
        );
    }

    private List<HistoryItem> loadApplicantApplicationItems(long currentUserId) {
        return jdbcTemplate.query(
            """
                SELECT pa.id, pa.application_status, pa.applied_at, p.id AS proposal_id, p.proposal_type, p.title
                FROM proposal_applications pa
                JOIN proposals p ON p.id = pa.proposal_id
                WHERE pa.applicant_user_id = ?
                  AND pa.deleted_at IS NULL
                  AND p.deleted_at IS NULL
                ORDER BY pa.applied_at DESC, pa.id DESC
                """,
            (rs, rowNum) -> new HistoryItem(
                "application-" + rs.getLong("id"),
                applicantStatusLabel(rs.getString("application_status")),
                rs.getString("proposal_type"),
                rs.getString("title"),
                applicantStatusSummary(rs.getString("application_status")),
                historyTime(rs.getTimestamp("applied_at")),
                timestampOrMin(rs.getTimestamp("applied_at")),
                proposalDetailPath(rs.getString("proposal_type"), rs.getLong("proposal_id"))
            ),
            currentUserId
        );
    }

    private List<HistoryItem> loadChatItems(long currentUserId) {
        return jdbcTemplate.query(
            """
                SELECT ct.id, ct.title, ct.latest_message_preview,
                       COALESCE(ct.latest_message_at, ct.updated_at, ct.created_at) AS occurred_at
                FROM chat_threads ct
                WHERE ct.deleted_at IS NULL
                  AND (ct.user_id = ? OR ct.counterpart_id = ?)
                ORDER BY COALESCE(ct.latest_message_at, ct.updated_at, ct.created_at) DESC, ct.id DESC
                """,
            (rs, rowNum) -> new HistoryItem(
                "chat-" + rs.getLong("id"),
                "Chat opened",
                "CHAT",
                rs.getString("title"),
                rs.getString("latest_message_preview") == null
                    ? "A chat thread linked to your involvement."
                    : rs.getString("latest_message_preview"),
                historyTime(rs.getTimestamp("occurred_at")),
                timestampOrMin(rs.getTimestamp("occurred_at")),
                "/app/chat/" + rs.getLong("id")
            ),
            currentUserId,
            currentUserId
        );
    }

    private List<HistoryItem> loadMapRecordItems(long currentUserId) {
        return jdbcTemplate.query(
            """
                SELECT mr.id, mr.title, mr.updated_at
                FROM map_records mr
                WHERE mr.user_id = ?
                  AND mr.deleted_at IS NULL
                ORDER BY mr.updated_at DESC, mr.id DESC
                """,
            (rs, rowNum) -> new HistoryItem(
                "map-" + rs.getLong("id"),
                "Map record",
                "MAP",
                rs.getString("title"),
                "A map record you saved.",
                historyTime(rs.getTimestamp("updated_at")),
                timestampOrMin(rs.getTimestamp("updated_at")),
                "/app/map-records/" + rs.getLong("id")
            ),
            currentUserId
        );
    }

    private List<HistoryItem> loadFootprintItems(long currentUserId) {
        return jdbcTemplate.query(
            """
                SELECT mr.id, mr.title, COALESCE(mr.visibility_updated_at, mr.updated_at, mr.created_at) AS occurred_at
                FROM map_records mr
                WHERE mr.user_id = ?
                  AND mr.deleted_at IS NULL
                  AND mr.is_draft = FALSE
                  AND mr.visibility IN ('public', 'limited')
                ORDER BY COALESCE(mr.visibility_updated_at, mr.updated_at, mr.created_at) DESC, mr.id DESC
                """,
            (rs, rowNum) -> new HistoryItem(
                "footprint-" + rs.getLong("id"),
                "Footprint visible",
                "FOOTPRINT",
                rs.getString("title"),
                "A record visible beyond your private map.",
                historyTime(rs.getTimestamp("occurred_at")),
                timestampOrMin(rs.getTimestamp("occurred_at")),
                "/app/map-records/" + rs.getLong("id")
            ),
            currentUserId
        );
    }

    private UserRow requireUser(String email) {
        try {
            return jdbcTemplate.queryForObject(
                """
                    SELECT id, email, nickname, name
                    FROM users
                    WHERE email = ?
                    """,
                (rs, rowNum) -> new UserRow(
                    rs.getLong("id"),
                    rs.getString("email"),
                    rs.getString("nickname"),
                    rs.getString("name")
                ),
                email
            );
        } catch (EmptyResultDataAccessException ex) {
            throw new IllegalStateException("User not found for history: " + email, ex);
        }
    }

    private String displayName(String nickname, String name, String email) {
        if (nickname != null && !nickname.isBlank()) {
            return nickname;
        }
        if (name != null && !name.isBlank()) {
            return name;
        }
        return email;
    }

    private LocalDateTime timestampOrMin(Timestamp timestamp) {
        return timestamp == null ? LocalDateTime.MIN : timestamp.toLocalDateTime();
    }

    private String historyTime(Timestamp timestamp) {
        LocalDateTime value = timestampOrMin(timestamp);
        return value == LocalDateTime.MIN ? "Time not set" : value.format(HISTORY_TIME);
    }

    private String proposalDetailPath(String proposalType, long proposalId) {
        if ("OKATTE".equalsIgnoreCase(proposalType)) {
            return "/app/okatte/" + proposalId;
        }
        return "/app/gate/" + proposalId;
    }

    private String applicantStatusLabel(String applicationStatus) {
        if ("accepted".equalsIgnoreCase(applicationStatus)) {
            return "Accepted";
        }
        if ("rejected".equalsIgnoreCase(applicationStatus)) {
            return "Postponed";
        }
        return "Applied";
    }

    private String applicantStatusSummary(String applicationStatus) {
        if ("accepted".equalsIgnoreCase(applicationStatus)) {
            return "An application that reached host-side approval.";
        }
        if ("rejected".equalsIgnoreCase(applicationStatus)) {
            return "An application currently treated as postponed.";
        }
        return "An application still waiting for a response.";
    }

    public record HistoryPageData(String currentUserLabel, List<HistoryItem> items) {
    }

    public record HistoryItem(
        String historyKey,
        String label,
        String kind,
        String title,
        String summary,
        String occurredAt,
        LocalDateTime occurredAtValue,
        String relatedUrl
    ) {
    }

    private record UserRow(long id, String email, String nickname, String name) {
    }
}
