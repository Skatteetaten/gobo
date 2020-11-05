CREATE TABLE FIELD (
--    ID       serial CONSTRAINT PK_FIELD PRIMARY KEY,
   NAME     VARCHAR(200) CONSTRAINT PK_FIELD PRIMARY KEY,,
   COUNT    INTEGER NOT NULL
);

CREATE TABLE FIELD_CLIENT (
--     ID       serial CONSTRAINT PK_FIELD_CLIENT PRIMARY KEY,
    FIELD_NAME INTEGER NOT NULL REFERENCES FIELD(NAME),
    NAME     VARCHAR(200) NOT NULL,
    COUNT    INTEGER NOT NULL
);