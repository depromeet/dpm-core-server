
create table if not exists attendances (
                                           attended_at datetime(6),
    id bigint not null auto_increment,
    member_id bigint not null,
    session_id bigint not null,
    status varchar(255) not null,
    primary key (id)
    ) engine=InnoDB;

create table if not exists authorities (
                                           authority_id bigint not null auto_increment,
                                           created_at datetime(6) not null,
    updated_at datetime(6) not null,
    name varchar(255) not null,
    primary key (authority_id)
    ) engine=InnoDB;

create table if not exists bill_accounts (
                                             bill_account_id bigint not null auto_increment,
                                             created_at datetime(6) not null,
    deleted_at datetime(6),
    updated_at datetime(6) not null,
    account_holder_name varchar(255) not null,
    account_type varchar(255) not null,
    bank_name varchar(255) not null,
    bill_account_value varchar(255) not null,
    primary key (bill_account_id)
    ) engine=InnoDB;

create table if not exists bills (
                                     bill_account_id bigint not null,
                                     bill_id bigint not null auto_increment,
                                     completed_at datetime(6),
    created_at datetime(6) not null,
    deleted_at datetime(6),
    host_user_id bigint,
    updated_at datetime(6) not null,
    bill_status varchar(255) not null,
    description varchar(255),
    title varchar(255) not null,
    primary key (bill_id)
    ) engine=InnoDB;

create table if not exists cohorts (
                                       cohort_id bigint not null auto_increment,
                                       created_at bigint not null,
                                       updated_at bigint not null,
                                       value varchar(255) not null,
    primary key (cohort_id)
    ) engine=InnoDB;

create table if not exists gathering_members (
                                                 is_checked bit not null,
                                                 is_joined bit not null,
                                                 completed_at datetime(6),
    created_at datetime(6) not null,
    deleted_at datetime(6),
    gathering_id bigint not null,
    gathering_member_id bigint not null auto_increment,
    member_id bigint not null,
    updated_at datetime(6) not null,
    primary key (gathering_member_id)
    ) engine=InnoDB;

create table if not exists gathering_receipt_photos (
                                                        created_at datetime(6) not null,
    deleted_at datetime(6),
    receipt_id bigint not null,
    receipt_photo_id bigint not null auto_increment,
    updated_at datetime(6) not null,
    url varchar(255) not null,
    primary key (receipt_photo_id)
    ) engine=InnoDB;

create table if not exists gathering_receipts (
                                                  amount integer not null,
                                                  split_amount integer,
                                                  created_at datetime(6) not null,
    deleted_at datetime(6),
    gathering_id bigint not null,
    receipt_id bigint not null auto_increment,
    updated_at datetime(6) not null,
    primary key (receipt_id)
    ) engine=InnoDB;

create table if not exists gatherings (
                                          round_number integer not null,
                                          bill_id bigint,
                                          created_at datetime(6) not null,
    deleted_at datetime(6),
    gathering_id bigint not null auto_increment,
    held_at datetime(6) not null,
    host_user_id bigint not null,
    updated_at datetime(6) not null,
    category varchar(255) not null,
    description varchar(255),
    title varchar(255) not null,
    primary key (gathering_id)
    ) engine=InnoDB;

create table if not exists member_authorities (
                                                  authority_id bigint not null,
                                                  deleted_at datetime(6),
    granted_at datetime(6) not null,
    member_authority_id bigint not null auto_increment,
    member_id bigint not null,
    primary key (member_authority_id)
    ) engine=InnoDB;

create table if not exists member_cohorts (
                                              cohort_id bigint not null,
                                              member_cohort_id bigint not null auto_increment,
                                              member_id bigint not null,
                                              primary key (member_cohort_id)
    ) engine=InnoDB;

create table if not exists member_oauth (
                                            member_id bigint not null,
                                            member_oauth_id bigint not null auto_increment,
                                            external_id varchar(255) not null,
    provider varchar(255) not null,
    primary key (member_oauth_id)
    ) engine=InnoDB;

create table if not exists member_teams (
                                            member_id bigint not null,
                                            member_team_id bigint not null auto_increment,
                                            team_id bigint not null,
                                            primary key (member_team_id)
    ) engine=InnoDB;

create table if not exists members (
                                       created_at datetime(6) not null,
    deleted_at datetime(6),
    member_id bigint not null auto_increment,
    updated_at datetime(6) not null,
    email varchar(255) not null,
    name varchar(255),
    part varchar(255),
    status varchar(255) not null,
    primary key (member_id)
    ) engine=InnoDB;

create table if not exists refresh_tokens (
                                              member_id bigint not null,
                                              token TEXT,
                                              primary key (member_id)
    ) engine=InnoDB;

create table if not exists session_attachments (
                                                   idx integer,
                                                   session_attachment_id bigint not null auto_increment,
                                                   session_id bigint not null,
                                                   path varchar(255) not null,
    title varchar(255) not null,
    primary key (session_attachment_id)
    ) engine=InnoDB;

create table if not exists sessions (
                                        is_online bit not null,
                                        week integer not null,
                                        attendance_start datetime(6) not null,
    cohort_id bigint not null,
    date datetime(6) not null,
    session_id bigint not null auto_increment,
    attendance_code varchar(255) not null,
    event_name varchar(255),
    place varchar(255),
    primary key (session_id)
    ) engine=InnoDB;

create table if not exists teams (
                                     number integer not null,
                                     cohort_id bigint not null,
                                     created_at bigint not null,
                                     team_id bigint not null auto_increment,
                                     updated_at bigint not null,
                                     primary key (team_id)
    ) engine=InnoDB;

alter table cohorts
    add constraint UKr63et36jlisakbvympgrb3c73 unique (value);

alter table gathering_receipts
    add constraint UKm6ed9w2qvce73flgh19rskk9t unique (gathering_id);

alter table member_oauth
    add constraint UKcl5n8qxo9j5ivyoxbrc5uhhxq unique (external_id);

alter table members
    add constraint UK9d30a9u1qpg8eou0otgkwrp5d unique (email);

alter table bills
    add constraint FKldoynwtbiwxk3bkgdq4282qh
        foreign key (bill_account_id)
            references bill_accounts (bill_account_id);

alter table gathering_members
    add constraint FK7vmlolgrbvgbymswh7qqhy5xe
        foreign key (gathering_id)
            references gatherings (gathering_id);

alter table gathering_receipt_photos
    add constraint FKsquibg6dtivur6mydhuupps5f
        foreign key (receipt_id)
            references gathering_receipts (receipt_id);

alter table gathering_receipts
    add constraint FK9hyt3g7h90ewfb6ce0njsypwf
        foreign key (gathering_id)
            references gatherings (gathering_id);
