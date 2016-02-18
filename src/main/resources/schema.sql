--
-- PostgreSQL database dump
--

-- Dumped from database version 9.5.0
-- Dumped by pg_dump version 9.5.0

-- Started on 2016-01-22 11:55:10

SET statement_timeout = 0;
SET lock_timeout = 0;
SET client_encoding = 'UTF8';
SET standard_conforming_strings = on;
SET check_function_bodies = false;
SET client_min_messages = warning;
SET row_security = off;

--
-- TOC entry 2128 (class 1262 OID 16393)
-- Name: sisdn2; Type: DATABASE; Schema: -; Owner: postgres
--

CREATE DATABASE sisdn5 WITH TEMPLATE = template0 ENCODING = 'UTF8' LC_COLLATE = 'English_United States.1252' LC_CTYPE = 'English_United States.1252';


ALTER DATABASE sisdn2 OWNER TO postgres;


SET search_path = public, pg_catalog;

SET default_tablespace = '';

SET default_with_oids = false;

--
-- TOC entry 183 (class 1259 OID 16426)
-- Name: courses; Type: TABLE; Schema: public; Owner: postgres
--

CREATE TABLE courses (
    id character varying NOT NULL,
    "departmentId" character varying NOT NULL,
    "facultyId" character varying NOT NULL,
    title character varying NOT NULL,
    "titleTr" character varying,
    remarks character varying,
    org character varying NOT NULL,
    "isActive" boolean NOT NULL
);


ALTER TABLE courses OWNER TO postgres;

--
-- TOC entry 182 (class 1259 OID 16418)
-- Name: departments; Type: TABLE; Schema: public; Owner: postgres
--

CREATE TABLE departments (
    id character varying NOT NULL,
    "facultyId" character varying NOT NULL,
    title character varying NOT NULL,
    "titleTr" character varying,
    org character varying NOT NULL,
    "isActive" boolean NOT NULL
);


ALTER TABLE departments OWNER TO postgres;

--
-- TOC entry 180 (class 1259 OID 16394)
-- Name: faculties; Type: TABLE; Schema: public; Owner: postgres
--

CREATE TABLE faculties (
    id character varying NOT NULL,
    title character varying NOT NULL,
    "titleTr" character varying,
    org character varying NOT NULL,
    "isActive" boolean NOT NULL
);


ALTER TABLE faculties OWNER TO postgres;

--
-- TOC entry 184 (class 1259 OID 16434)
-- Name: programs; Type: TABLE; Schema: public; Owner: postgres
--

CREATE TABLE programs (
    id character varying NOT NULL,
    "facultyId" character varying NOT NULL,
    terms integer NOT NULL,
    "creditHours" numeric(21,2) NOT NULL,
    title character varying NOT NULL,
    "titleTr" character varying,
    org character varying NOT NULL,
    "isActive" boolean NOT NULL
);


ALTER TABLE programs OWNER TO postgres;

--
-- TOC entry 181 (class 1259 OID 16402)
-- Name: streamOffsets; Type: TABLE; Schema: public; Owner: postgres
--

CREATE TABLE "streamOffsets" (
    id character varying NOT NULL,
    "offset" bigint NOT NULL
);


ALTER TABLE "streamOffsets" OWNER TO postgres;

--
-- TOC entry 2007 (class 2606 OID 16433)
-- Name: courses_pk; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY courses
    ADD CONSTRAINT courses_pk PRIMARY KEY (id, org);


--
-- TOC entry 2005 (class 2606 OID 16425)
-- Name: departments_pk; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY departments
    ADD CONSTRAINT departments_pk PRIMARY KEY (id, org);


--
-- TOC entry 2001 (class 2606 OID 16401)
-- Name: faculties_pk; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY faculties
    ADD CONSTRAINT faculties_pk PRIMARY KEY (id, org);


--
-- TOC entry 2009 (class 2606 OID 16441)
-- Name: programs_pk; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY programs
    ADD CONSTRAINT programs_pk PRIMARY KEY (id, org);


--
-- TOC entry 2003 (class 2606 OID 16409)
-- Name: streamOffsets_pkey; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY "streamOffsets"
    ADD CONSTRAINT "streamOffsets_pkey" PRIMARY KEY (id);


--
-- TOC entry 2130 (class 0 OID 0)
-- Dependencies: 5
-- Name: public; Type: ACL; Schema: -; Owner: postgres
--

REVOKE ALL ON SCHEMA public FROM PUBLIC;
REVOKE ALL ON SCHEMA public FROM postgres;
GRANT ALL ON SCHEMA public TO postgres;
GRANT ALL ON SCHEMA public TO PUBLIC;


-- Completed on 2016-01-22 11:55:10

--
-- PostgreSQL database dump complete
--

