create table courses (
                         id bigserial primary key,
                         user_id bigint not null,
                         region_id bigint not null,
                         title varchar(255) not null,
                         status varchar(30) not null,
                         created_at timestamp not null,
                         updated_at timestamp not null,

                         constraint fk_courses_user
                             foreign key (user_id)
                                 references users(id),

                         constraint fk_courses_region
                             foreign key (region_id)
                                 references regions(id)
);

create table course_places (
                               id bigserial primary key,
                               course_id bigint not null,
                               place_id bigint not null,
                               sequence integer not null,
                               category_group varchar(30) not null,
                               category_detail varchar(30),
                               created_at timestamp not null,

                               constraint fk_course_places_course
                                   foreign key (course_id)
                                       references courses(id)
                                       on delete cascade,

                               constraint fk_course_places_place
                                   foreign key (place_id)
                                       references places(id),

                               constraint uk_course_places_course_place
                                   unique (course_id, place_id),

                               constraint uk_course_places_course_category_group
                                   unique (course_id, category_group),

                               constraint uk_course_places_course_sequence
                                   unique (course_id, sequence)
);

create index idx_courses_user_status
    on courses(user_id, status);

create unique index uk_courses_user_active
    on courses(user_id)
    where status = 'ACTIVE';

create index idx_course_places_course_id
    on course_places(course_id);