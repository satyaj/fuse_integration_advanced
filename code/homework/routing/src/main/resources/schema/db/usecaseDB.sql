1) CREATE USECASE SCHEMA
************************

CREATE SCHEMA USECASE;

2) CREATE TABLES FOR USECASE SCHEMAS
***********************************

CREATE TABLE USECASE.T_ACCOUNT (
    ID BIGINT GENERATED BY DEFAULT AS IDENTITY(START WITH 1) NOT NULL PRIMARY KEY,
    CLIENT_ID BIGINT,
    SALES_CONTACT VARCHAR(30),
    COMPANY_NAME VARCHAR(50),
    COMPANY_GEO CHAR(20) ,
    COMPANY_ACTIVE BOOLEAN,
    CONTACT_FIRST_NAME VARCHAR(35),
    CONTACT_LAST_NAME VARCHAR(35),
    CONTACT_ADDRESS VARCHAR(255),
    CONTACT_CITY VARCHAR(40),
    CONTACT_STATE VARCHAR(40),
    CONTACT_ZIP VARCHAR(10),
    CONTACT_EMAIL VARCHAR(60),
    CONTACT_PHONE VARCHAR(35),
    CREATION_DATE TIMESTAMP,
    CREATION_USER VARCHAR(255)
);


3) INSERT RECORDS
*****************
INSERT INTO USECASE.T_ACCOUNT (CLIENT_ID,SALES_CONTACT,COMPANY_NAME,COMPANY_GEO,COMPANY_ACTIVE,CONTACT_FIRST_NAME,CONTACT_LAST_NAME,CONTACT_ADDRESS,CONTACT_CITY,CONTACT_STATE,CONTACT_ZIP,CONTACT_PHONE,CREATION_DATE,CREATION_USER) VALUES ('95','Rachel Cassidy','MountainBikers','SOUTH_AMERICA',true,'George','Jungle','1101 Smith St.','Raleigh','NC','27519','919-555-0800','2015-12-15','fuse_usecase');


6) Useful scripts
******************
DELETE FROM USECASE.T_ACCOUNT;

SELECT * FROM USECASE.T_ACCOUNT;

DROP SCHEMA USECASE;
