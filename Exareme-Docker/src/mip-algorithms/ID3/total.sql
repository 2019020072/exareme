--LOCAL1

update defaultDB.algorithmparameters set val=val+1 where name ='iterations';

--For each categorical column x:
--segment the data by the distinct values of each column, and by the class values, and then count the rows.
var 'columns1' from getschema outputformat:1 select * from defaultDB.localinputtblcurrent;

drop table if exists localcounts;
create table localcounts (colname text, val text, classval text, quantity int);
var  'a' from select create_complex_query(""," insert into localcounts select '?', `?`, `%{y}`, count(*)
from  defaultDB.localinputtblcurrent group by `?`, `%{y}`; ", "" , "" , '%{columns1}');
%{a};

select * from localcounts;


--GLOBAL1


var 'input_global_tbl' 'localcounts';
----------------------------------------------------------------------------------------------------------
-- Merge local_counts
drop table if exists defaultDB.globalcounts;
create table  defaultDB.globalcounts as
select  colname, val, classval, sum(quantity) as quantity
from %{input_global_tbl}
group by colname, classval,val;

--Compute gain
drop table if exists defaultDB.gain;
create table defaultDB.gain as
select colname, max(sumofentropies) from (
    select colname, sum(nentropy)/sum(n) as sumofentropies
    from( select colname, val, n,  sumnlong - n* pyfun('math.log', n ,2)  as nentropy
          from( select colname, val, sum(quantity) as n, sum(quantity * pyfun('math.log', quantity ,2)) as sumnlong
                from defaultDB.globalcounts
                group by colname, val )
          where colname!=var('y')
        )
    group by colname
);

var 'distinctvalues' from select group_concat(val) from (select distinct %{y} as val from defaultDB.localinputtblcurrent);
var 'noofcolumns' from select count(*) from (coltypes select * from defaultdb.localinputtblcurrent);

--2. Find new nodes of tree and update global_tree
drop table if exists defaultDB.globalnewnodesoftree;
create table defaultDB.globalnewnodesoftree (no int, colname text, colval text, nextnode int, leafval text);
insert into  globalnewnodesoftree
select no, colname, colval, nextnode, leafval
from ( select distinct colname, val as colval, case when count(*) = 1 then "-" else "?" end as nextnode,  case when count(*) == 1 then classval else "?" end as  leafval
       from defaultDB.globalcounts where colname in (select colname from defaultDB.gain)
       group by colname,val),
     ( select case when no is null then '1' else cast(max(no)+1 as text)   end as no from defaultdb.globaltree);
select * from defaultDB.globalnewnodesoftree;

update defaultDB.globalnewnodesoftree
set leafval= '%{distinctvalues}' where leafval='?' and %{noofcolumns}=2;
select * from defaultDB.globalnewnodesoftree;


update defaultdb.globaltree set nextnode = (select no from defaultDB.globalnewnodesoftree)
where  jmerge(no,colname,colval) is (select jmerge (no,colname,colval) from defaultDB.globalpathforsplittree);

insert into defaultdb.globaltree select * from defaultDB.globalnewnodesoftree;

--3. Find the path in order to split the input dataset/table
drop table if exists defaultDB.globalpathforsplittree;
create table defaultDB.globalpathforsplittree as
pathtree value:? select * from defaultdb.globaltree;

select * from defaultDB.globalpathforsplittree;

--LOCAL2

var 'prv_output_global_tbl' 'globalpathforsplittree';

--- Split initial dataset based on global_pathforsplittree
var 'filters' from select tabletojson(colname,colval, "colname,val") from %{prv_output_global_tbl};
drop table if exists defaultDB.localinputtblcurrent;
create table defaultDB.localinputtblcurrent as
filtertable filters:%{filters} select * from defaultDB.localinputtbl;

select 'ok';


--GLOBAl2

select * from defaultDB.globaltree;
