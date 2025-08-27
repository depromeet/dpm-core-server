-- dpm_core.attendances definition

CREATE TABLE `attendances`
(
    `attendance_id` bigint       NOT NULL AUTO_INCREMENT,
    `attended_at`   timestamp(6) DEFAULT NULL,
    `member_id`     bigint       NOT NULL,
    `session_id`    bigint       NOT NULL,
    `status`        varchar(255) NOT NULL,
    PRIMARY KEY (`attendance_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;


-- dpm_core.authorities definition

CREATE TABLE `authorities`
(
    `authority_id` bigint       NOT NULL AUTO_INCREMENT,
    `created_at`   bigint       NOT NULL,
    `name`         varchar(255) NOT NULL,
    `updated_at`   bigint       NOT NULL,
    PRIMARY KEY (`authority_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;


-- dpm_core.bill_accounts definition

CREATE TABLE `bill_accounts`
(
    `bill_account_id`   bigint       NOT NULL AUTO_INCREMENT,
    `bill_account_info` varchar(255) NOT NULL,
    `created_at`        datetime(6) NOT NULL,
    `deleted_at`        datetime(6) DEFAULT NULL,
    `is_url`            bit(1)       NOT NULL,
    `name`              varchar(255) NOT NULL,
    `updated_at`        datetime(6) NOT NULL,
    PRIMARY KEY (`bill_account_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;


-- dpm_core.cohorts definition

CREATE TABLE `cohorts`
(
    `cohort_id`  bigint       NOT NULL AUTO_INCREMENT,
    `created_at` bigint       NOT NULL,
    `updated_at` bigint       NOT NULL,
    `value`      varchar(255) NOT NULL,
    PRIMARY KEY (`cohort_id`),
    UNIQUE KEY (`value`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;


-- dpm_core.member_authorities definition

CREATE TABLE `member_authorities`
(
    `member_authority_id` bigint NOT NULL AUTO_INCREMENT,
    `deleted_at`          datetime(6) DEFAULT NULL,
    `granted_at`          datetime(6) NOT NULL,
    `authority_id`        bigint NOT NULL,
    `member_id`           bigint NOT NULL,
    PRIMARY KEY (`member_authority_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;


-- dpm_core.member_cohorts definition

CREATE TABLE `member_cohorts`
(
    `member_cohort_id` bigint NOT NULL AUTO_INCREMENT,
    `cohort_id`        bigint NOT NULL,
    `member_id`        bigint NOT NULL,
    PRIMARY KEY (`member_cohort_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;


-- dpm_core.member_oauth definition

CREATE TABLE `member_oauth`
(
    `member_oauth_id` bigint       NOT NULL AUTO_INCREMENT,
    `external_id`     varchar(255) NOT NULL,
    `provider`        varchar(255) NOT NULL,
    `member_id`       bigint       NOT NULL,
    PRIMARY KEY (`member_oauth_id`),
    UNIQUE KEY (`external_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;


-- dpm_core.member_teams definition

CREATE TABLE `member_teams`
(
    `member_team_id` bigint NOT NULL AUTO_INCREMENT,
    `member_id`      bigint NOT NULL,
    `team_id`        bigint NOT NULL,
    PRIMARY KEY (`member_team_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;


-- dpm_core.members definition

CREATE TABLE `members`
(
    `member_id`  bigint       NOT NULL AUTO_INCREMENT,
    `created_at` datetime(6) NOT NULL,
    `deleted_at` datetime(6) DEFAULT NULL,
    `email`      varchar(255) NOT NULL,
    `name`       varchar(255) DEFAULT NULL,
    `part`       varchar(255) DEFAULT NULL,
    `status`     varchar(255) NOT NULL,
    `updated_at` datetime(6) NOT NULL,
    PRIMARY KEY (`member_id`),
    UNIQUE KEY (`email`)
) ENGINE=InnoDB AUTO_INCREMENT=8 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;


-- dpm_core.refresh_tokens definition

CREATE TABLE `refresh_tokens`
(
    `member_id` bigint NOT NULL,
    `token`     text,
    PRIMARY KEY (`member_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;


-- dpm_core.session_attachments definition

CREATE TABLE `session_attachments`
(
    `session_attachment_id` bigint       NOT NULL AUTO_INCREMENT,
    `idx`                   int DEFAULT NULL,
    `path`                  varchar(255) NOT NULL,
    `title`                 varchar(255) NOT NULL,
    `session_id`            bigint       NOT NULL,
    PRIMARY KEY (`session_attachment_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;


-- dpm_core.sessions definition

CREATE TABLE `sessions`
(
    `session_id`       bigint       NOT NULL AUTO_INCREMENT,
    `attendance_code`  varchar(255) NOT NULL,
    `attendance_start` timestamp(6) NOT NULL,
    `cohort_id`        bigint       NOT NULL,
    `date`             timestamp(6) NOT NULL,
    `event_name`       varchar(255) DEFAULT NULL,
    `is_online`        bit(1)       NOT NULL,
    `place`            varchar(255) DEFAULT NULL,
    `week`             int          NOT NULL,
    PRIMARY KEY (`session_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;


-- dpm_core.teams definition

CREATE TABLE `teams`
(
    `team_id`    bigint NOT NULL AUTO_INCREMENT,
    `created_at` bigint NOT NULL,
    `number`     int    NOT NULL,
    `updated_at` bigint NOT NULL,
    `cohort_id`  bigint NOT NULL,
    PRIMARY KEY (`team_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;


-- dpm_core.bills definition

CREATE TABLE `bills`
(
    `bill_id`         bigint       NOT NULL AUTO_INCREMENT,
    `completed_at`    datetime(6) DEFAULT NULL,
    `created_at`      datetime(6) NOT NULL,
    `deleted_at`      datetime(6) DEFAULT NULL,
    `description`     varchar(255) DEFAULT NULL,
    `title`           varchar(255) NOT NULL,
    `updated_at`      datetime(6) NOT NULL,
    `bill_account_id` bigint       NOT NULL,
    PRIMARY KEY (`bill_id`),
    CONSTRAINT FOREIGN KEY (`bill_account_id`) REFERENCES `bill_accounts` (`bill_account_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;


-- dpm_core.gatherings definition

CREATE TABLE `gatherings`
(
    `gathering_id` bigint       NOT NULL AUTO_INCREMENT,
    `category`     varchar(255) NOT NULL,
    `created_at`   datetime(6) NOT NULL,
    `deleted_at`   datetime(6) DEFAULT NULL,
    `description`  varchar(255) DEFAULT NULL,
    `held_at`      datetime(6) NOT NULL,
    `host_user_id` bigint       NOT NULL,
    `round_number` int          NOT NULL,
    `title`        varchar(255) NOT NULL,
    `updated_at`   datetime(6) NOT NULL,
    `bill_id`      bigint       NOT NULL,
    PRIMARY KEY (`gathering_id`),
    CONSTRAINT FOREIGN KEY (`bill_id`) REFERENCES `bills` (`bill_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;


-- dpm_core.receipts definition

CREATE TABLE `gathering_receipts`
(
    `gathering_receipt_id` bigint NOT NULL AUTO_INCREMENT,
    `amount`               int    NOT NULL,
    `created_at`           datetime(6) NOT NULL,
    `deleted_at`           datetime(6) DEFAULT NULL,
    `split_amount`         int    NOT NULL,
    `updated_at`           datetime(6) NOT NULL,
    `gathering_id`         bigint NOT NULL,
    PRIMARY KEY (`gathering_receipt_id`),
    CONSTRAINT FOREIGN KEY (`gathering_id`) REFERENCES `gatherings` (`gathering_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;


-- dpm_core.gathering_members definition

CREATE TABLE `gathering_members`
(
    `gathering_member_id` bigint NOT NULL AUTO_INCREMENT,
    `completed_at`        datetime(6) DEFAULT NULL,
    `created_at`          datetime(6) NOT NULL,
    `deleted_at`          datetime(6) DEFAULT NULL,
    `is_viewed`          bit(1) NOT NULL,
    `is_joined`           bit(1) NOT NULL,
    `is_invitation_submitted` bit(1) NOT NULL,
    `memo`                  text NULL,
    `member_id`           bigint NOT NULL,
    `updated_at`          datetime(6) NOT NULL,
    `gathering_id`        bigint NOT NULL,
    PRIMARY KEY (`gathering_member_id`),
    CONSTRAINT FOREIGN KEY (`gathering_id`) REFERENCES `gatherings` (`gathering_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;


-- dpm_core.receipt_photos definition

CREATE TABLE `gathering_receipt_photos`
(
    `gathering_receipt_photo_id` bigint       NOT NULL AUTO_INCREMENT,
    `created_at`                 datetime(6) NOT NULL,
    `deleted_at`                 datetime(6) DEFAULT NULL,
    `updated_at`                 datetime(6) NOT NULL,
    `url`                        varchar(255) NOT NULL,
    `gathering_receipt_id`       bigint       NOT NULL,
    PRIMARY KEY (`gathering_receipt_photo_id`),
    CONSTRAINT `FKqe3p13w642a74nca9hsmmr9rt` FOREIGN KEY (`gathering_receipt_id`) REFERENCES `gathering_receipts` (`gathering_receipt_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
