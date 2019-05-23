
------------------Input for testing
------------------------------------------------------------------------------
--Test 1
drop table if exists inputdata;
create table inputdata as
   select *
   from (file header:t '/home/eleni/Desktop/HBP/exareme/Exareme-Docker/src/mip-algorithms/unit_tests/datasets/CSVs/Naive/BayesNaiveTestDataset.csv');

var 'x' 'outlook,temperature,humidity,windy';
var 'y' 'play';
var 'kfold' 3;
var 'defaultDB' 'mydefaultDB.db';
attach 'datasets.db' as localDB;

-- 'Iris_dataset'
--var 'defaultDB' 'defaultDB';
--var 'columns' 'Sepal_Length,Sepal_Width,Petal_Length,Petal_Width';
--var 'classname' 'class';
--var 'kfold' 2;
--var 'alpha' 1;


-------------------------------------------------------------------------------------
requirevars 'defaultDB' 'input_local_DB' 'db_query' 'x' 'y' 'kfold'; -- y = classname
attach database '%{defaultDB}' as defaultDB;
--attach database '%{input_local_DB}' as localDB;

--var 'min_k_data_aggregation' 5;

--Read dataset
--drop table if exists inputdata;
--create table inputdata as select * from (%{db_query});

-- Delete patients with null values (val is null or val = '' or val = 'NA'). Cast values of columns using cast function.
var 'nullCondition' from select create_complex_query(""," ? is not null and ? <>'NA' and ? <>'' ", "and" , "" , '%{x},%{y}');
var 'cast_x' from select create_complex_query("","tonumber(?) as ?", "," , "" , '%{x}');--TODO!!!!
drop table if exists inputdata2;
create table inputdata2 as
select %{cast_x}, tonumber(%{y}) as '%{y}' from inputdata where %{nullCondition};

-- Add a new column: "idofset". It is used in order to split dataset in training and test datasets.
drop table if exists defaultDB.localinputtblflat;
create table defaultDB.localinputtblflat as
select %{x},%{y}, kfold.idofset as idofset
from inputdata2  as h, (sklearnkfold splits:%{kfold} select rowid from inputdata2) as kfold
where kfold.rid = h.rowid;

drop table if exists defaultDB.metadatatbl;
create table defaultDB.metadatatbl as
select * from metadata where code in (select strsplitv('%{x}','delimiter:,')) or code ='%{y}';

select * from defaultDB.metadatatbl;
