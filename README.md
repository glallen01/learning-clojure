# learning clojure by parsing bro logs

[src/stuff/core.clj](src/stuff/core.clj) reads a bro-log (in this case a short, sample [conn.log](conn.log)), grabs fields from the headers, and turns it into a clojure map (ie hashtable) of the fields.

So far, reading the file and grabbing the column names works.

## TODO:

    + extract the types column 
    - cast the fields
    + slice out columns a la bro-cut
    - threads
    + read compressed files
    - set loose on a whole dir of brologs as a mulithreaded log-chewing beast
    - build some hunts
    - use mmap?

## Usage

    $ lein run

or 

    $ lein uberjar; java -jar target/uberjar/stuff-0.1.0-SNAPSHOT-standalone.jar

## Options

none yet

## Examples

```
~/src/clj/stuff (master *%) $ lein run
Separator: \x09
Fields: [ts uid id.orig_h id.orig_p id.resp_h id.resp_p proto service duration orig_bytes resp_bytes conn_state local_orig local_resp missed_bytes history orig_pkts orig_ip_bytes resp_pkts resp_ip_bytes tunnel_parents]
Ten lines from connlog:
(#separator \x09 #set_separator	, #empty_field 	(empty) #unset_field   	- #path	conn #open     	2016-09-05-00-36-26 #fields    	ts     	uid    	id.orig_h      	id.orig_p      	id.resp_h      	id.resp_p	proto   	service	duration       	orig_bytes     	resp_bytes     	conn_state     	local_orig     	local_resp     	missed_bytes   	history	orig_pkts      	orig_ip_bytes  	resp_pkts      	resp_ip_bytes  	tunnel_parents #types     	time   	string 	addr   	port   	addr   	port   	enum   	string 	interval       	count  	count  	string 	bool   	bool   	count  	string 	count  	count  	count  	count  	set[string] 1265153646.956656     	CTNhWv4n6L3QwumUO3     	10.42.42.253   	46104  	10.42.42.50    	80     	tcp    	-      	0.000731       	0      	0      	REJ    	-      	-      	0      	Sr     	1      	60     	1	40      	(empty) 1265153647.564250      	CnMCVB4RrA2F8HziU1     	10.42.42.253   	59856  	10.42.42.56    	80     	tcp    	-      	0.000085       	0      	0      	REJ    	-      	-      	0      	Sr     	1	60      	1      	40     	(empty))

attempting (bro-process-log)...
{:service -, :local_orig -, :orig_pkts 1, :id.resp_p 80, :uid CTNhWv4n6L3QwumUO3, :history Sr, :local_resp -, :resp_ip_bytes 40, :duration 0.000731, :tunnel_parents (empty), :ts 1265153646.956656, :id.orig_h 10.42.42.253, :proto tcp, :orig_ip_bytes 60, :id.orig_p 46104, :orig_bytes 0, :resp_pkts 1, :conn_state REJ, :resp_bytes 0, :missed_bytes 0, :id.resp_h 10.42.42.50}
{:service -, :local_orig -, :orig_pkts 1, :id.resp_p 80, :uid CnMCVB4RrA2F8HziU1, :history Sr, :local_resp -, :resp_ip_bytes 40, :duration 0.000085, :tunnel_parents (empty), :ts 1265153647.564250, :id.orig_h 10.42.42.253, :proto tcp, :orig_ip_bytes 60, :id.orig_p 59856, :orig_bytes 0, :resp_pkts 1, :conn_state REJ, :resp_bytes 0, :missed_bytes 0, :id.resp_h 10.42.42.56}
{:service -, :local_orig -, :orig_pkts 1, :id.resp_p 80, :uid CJHbCx3YSCeLUt5Jl2, :history Sr, :local_resp -, :resp_ip_bytes 40, :duration 0.000173, :tunnel_parents (empty), :ts 1265153647.564252, :id.or
```

