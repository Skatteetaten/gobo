CREATE TABLE FIELD (
   NAME     VARCHAR(200) CONSTRAINT PK_FIELD PRIMARY KEY,
   COUNT    INTEGER NOT NULL
);

CREATE TABLE FIELD_CLIENT (
    FIELD_NAME VARCHAR(200) NOT NULL REFERENCES FIELD(NAME),
    NAME     VARCHAR(200) NOT NULL,
    COUNT    INTEGER NOT NULL
);

--
-- CREATE TABLE FIELD (
--                        ID       serial CONSTRAINT PK_FIELD PRIMARY KEY,
--                        NAME     VARCHAR(200) NOT NULL,
--                        COUNT    INTEGER NOT NULL
-- );
--
-- CREATE TABLE FIELD_CLIENT (
--                               FIELD_ID  INTEGER NOT NULL REFERENCES FIELD(ID),
--                               NAME     VARCHAR(200) NOT NULL,
--                               COUNT    INTEGER NOT NULL
-- );