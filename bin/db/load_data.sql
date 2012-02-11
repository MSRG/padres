--
-- This is a script file used for populate padres database tables
--
-- Host: colossus.ece.utoronto.ca Database: padres
---------------------------------------------------------
-- PostgreSQL Server version 7.3.2 

-- @author Pengcheng Wan
-- @version 1.1

--INSERT INTO classes (Class_Name) VALUES ('stock');

--INSERT INTO attributes (Attribute_Name, Attribute_Type) VALUES ('volume', 'Long');
--INSERT INTO attributes (Attribute_Name, Attribute_Type) VALUES ('price', 'Double');
--INSERT INTO attributes (Attribute_Name, Attribute_Type) VALUES ('code', 'String');

--INSERT INTO longvalues VALUES (1, 100);

--INSERT INTO doublevalues  VALUES (2, 500.00);

--INSERT INTO stringvalues  VALUES (3, 'CYB');

--INSERT INTO pairs VALUES (1, 1, 1);
--INSERT INTO pairs VALUES (2, 3, 3);
--INSERT INTO pairs VALUES (3, 2, 2);

--INSERT INTO eventdata VALUES (1, 1);
--INSERT INTO eventdata VALUES (1, 2);
--INSERT INTO eventdata VALUES (2, 3);
--INSERT INTO eventdata VALUES (2, 2);


--INSERT INTO events VALUES ('1', 1, 1, '66.218.70.49',null,1,now());
--INSERT INTO events VALUES ('2', 1, 2, '128.100.241.80',null,1,now());


---------------------------------------------------------
-- Create view for database query
--
--CREATE VIEW testView AS 
(SELECT DISTINCT
E.Event_ID, C.Class_Name, A.Attribute_Name,
A.Attribute_Type, P.Long_ValueID, P.Double_ValueID, P.String_ValueID
FROM events AS E, eventdata AS ED, classes AS C, pairs AS P, attributes AS A,
     longvalues AS IV, doublevalues AS DV, stringvalues AS SV 
WHERE E.Event_ID = 'Pub-2' AND E.Class_ID = C.Class_ID AND E.Data_ID = ED.Data_ID AND 
ED.Pair_ID = P.Pair_ID AND P.Attribute_ID = A.Attribute_ID );
--AND ((P.Long_ValueID = IV.Value_ID AND A.Attribute_Type = 'Long  ') OR 
--     (P.Double_ValueID = DV.Value_ID AND A.Attribute_Type = 'Double') OR 
--     (P.String_ValueID = SV.Value_ID AND A.Attribute_Type = 'String')));
