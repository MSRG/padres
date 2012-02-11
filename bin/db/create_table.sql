--
-- This is a script file used for creating padres database
-- which has eight tables for historical data.
--
-- Host: colossus.ece.utoronto.ca Database: padres
---------------------------------------------------------
-- PostgreSQL Server version 7.3.2 

-- @author Pengcheng Wan
-- @version 1.3

---------------------------------------------------------
--
-- Table structure for table 'CLASSES'
--
CREATE TABLE classes (
  Class_ID serial,                    -- autoincrement field
  Class_Name varchar(64) NOT NULL,
  PRIMARY KEY (Class_ID)
);

--
-- Table structure for table 'ATTRIBUTES'
--
CREATE TABLE attributes (
  Attribute_ID serial,                  -- autoincrement field
  Attribute_Name varchar(64) NOT NULL ,
  Attribute_Type char(6) NOT NULL,     -- enum from {Long, Double, String}
  PRIMARY KEY  (Attribute_ID)
);

--
-- Table structure for table 'INTEGERVALUES'
--
CREATE TABLE longvalues (
  Value_ID serial,						-- autoincrement field         
  Long_Value integer NOT NULL,
  PRIMARY KEY  (Value_ID)
);

--
-- Table structure for table 'DOUBLEVALUES'
--
CREATE TABLE doublevalues (
  Value_ID serial,	 					-- autoincrement field
  Double_Value float NOT NULL,
  PRIMARY KEY  (Value_ID)
);

--
-- Table structure for table 'STRINGVALUES'
--
CREATE TABLE stringvalues (
  Value_ID serial,						-- autoincrement field
  String_Value char(30)   NOT NULL,
  PRIMARY KEY  (Value_ID)
);

--
-- Table structure for table 'PAIRS'
--
CREATE TABLE pairs (
  Pair_ID integer NOT NULL,
  Attribute_ID integer NOT NULL,
  Long_ValueID integer,
  Double_ValueID integer,
  String_ValueID integer,
  PRIMARY KEY (Pair_ID),
  FOREIGN KEY (Attribute_ID) REFERENCES attributes (Attribute_ID) ON DELETE SET NULL
--FOREIGN KEY (Long_ValueID) REFERENCES longvalues (Value_ID) ON DELETE SET NULL,
--FOREIGN KEY (Double_ValueID) REFERENCES doublevalues (Value_ID) ON DELETE SET NULL,
--FOREIGN KEY (String_ValueID) REFERENCES stringvalues (Value_ID) ON DELETE SET NULL
);

--
-- Table structure for table 'EVENTDATA'
--
CREATE TABLE eventdata (
  Data_ID integer NOT NULL,
  Pair_ID integer NOT NULL,
  PRIMARY KEY (Data_ID, Pair_ID),
  FOREIGN KEY (Pair_ID) REFERENCES pairs (Pair_ID) ON DELETE SET NULL
);

--
-- Table structure for table 'EVENTS'
--
CREATE TABLE events (
  Event_ID varchar(128) NOT NULL,
  Class_ID integer NOT NULL,
  Data_ID integer  NOT NULL,
  LastHop_ID varchar(128) NOT NULL,
  Payload bytea,	-- NOT NULL??
  Priority smallint NOT NULL,
  Time timestamp(6) NOT NULL,
  PRIMARY KEY (Event_ID),
  FOREIGN KEY (Class_ID) REFERENCES classes (Class_ID) ON DELETE SET NULL
--FOREIGN KEY (Data_ID) REFERENCES eventdata (Data_ID) ON DELETE SET NULL  
);

---------------------------------------------------------
--shou, Mar 17,2007, old view
-- Create view for database query
--
--CREATE VIEW mess AS ( SELECT DISTINCT
--E.Event_ID, C.Class_Name, A.Attribute_Name,
--A.Attribute_Type, IV.Long_Value, DV.Double_Value, SV.String_Value
--FROM events AS E, eventdata AS ED, classes AS C, pairs AS P, attributes AS A,
--     longvalues AS IV, doublevalues AS DV, stringvalues AS SV 
--WHERE E.Class_ID = C.Class_ID AND E.Data_ID = ED.Data_ID AND 
--ED.Pair_ID = P.Pair_ID AND P.Attribute_ID = A.Attribute_ID
--AND ((P.Long_ValueID = IV.Value_ID AND A.Attribute_Type = 'Long  ') OR 
--     (P.Double_ValueID = DV.Value_ID AND A.Attribute_Type = 'Double') OR 
--     (P.String_ValueID = SV.Value_ID AND A.Attribute_Type = 'String')));

-------------------------------------------------------------
-- shou, Mar 19,2007, fix the bug that the publication inserted into the database does not
-- have the longvalues or doublevalues attribute.
-- Create view for database query
--
CREATE VIEW messages AS ( SELECT DISTINCT
E.Event_ID, C.Class_Name, A.Attribute_Name, A.Attribute_Type, IV.Long_Value, SV.String_Value, DV.Double_Value
FROM (events AS E JOIN classes AS C ON E.Class_ID = C.Class_ID) JOIN eventdata AS ED
     ON (E.Data_ID = ED.Data_ID) JOIN pairs AS P 
     ON (ED.Pair_ID = P.Pair_ID) JOIN attributes AS A 
     ON (P.Attribute_ID = A.Attribute_ID) LEFT OUTER JOIN stringvalues AS SV 
     ON (P.String_ValueID = SV.Value_ID AND A.Attribute_Type = 'String') LEFT OUTER JOIN longvalues AS IV 
     ON (P.Long_ValueID = IV.Value_ID AND A.Attribute_Type = 'Long') LEFT OUTER JOIN doublevalues AS DV
     ON (P.Double_ValueID = DV.Value_ID AND A.Attribute_Type = 'Double'));
    
