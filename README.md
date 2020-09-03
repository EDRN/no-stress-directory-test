# 😌 No Stress

This is a simple low-stress (really, "no stress") test of [Apache Directory Service](https://directory.apache.org/) from Java, in order to reveal problems with the [LabCAS Backend](https://github.com/EDRN/labcas-backend/tree/master/data-access-api/src/gov/nasa/jpl/labcas/data_access_api) and ~~its continual struggles accessing a newer version of the directory~~ just how broken the new Apache Directory Service is.

The issue is that the LabCAS data access API eventually hangs while servicing a request. The following script demonstrates the problem:


```bash
while :; do
    curl --silent --user 'kelly:x' 'http://localhost:8080/labcas-backend-data-access-api/auth'
done
```

The LabCAS backend on handling the `/labcas-backend-data-access-api/auth` endpoint is configured to contact two different Apache DS instances with roughly the same data. The results of this contact is:

-   Using `ldaps://edrn-ds.jpl.nasa.gov` (running ApacheDS 2.0.0.AM25), the service hangs (accepts connections but returns no data) after 20 to 100 requests.
-   Using `ldaps://edrn.jpl.nasa.gov` (running ApaheDS 2.0.0-M20), the service works after 8000+ requests.

What could be causing this difference? 🤔


## 🩺 No-Stress Test Case

The `main` routine in this package imitates what LabCAS does; in essence it performs the following operations:

1. LDAP `bind` using the manager DN `uid=admin,ou=system` and its password in "simple" mode.
2. LDAP `search` for a user (in thie case, `uid=kelly`) and retrieval of his DN with the session established in ①.
3. LDAP `bind` using that user's DN discovered in ② and an invalid password, namely `x`.

There are no concurrent operations of any kind; just the above three, sequentially, over and over.

To run the test case:

    java -jar no-stress-0.0.0.jar WHICH [FREQUENCY]

where

-   WHICH is `old` or `new`
    -   `old` means to use `ldaps://edrn.jpl.nasa.gov` with its old manager password
    -   `new` means to use `ldaps://edrn-ds.jpl.nasa.gov` with its new manager passowrd
-   FREQUENCY is how often to report success, defaults to every 10 times


## 🤷‍♀️ Old versus New

In the below data, *no concurrent* tests were run. Instead, tests occurred sequentially and from *one host at a time* so as not to stress the LDAP servers. When a number appears, that indicates how many successful tests were run in that trial before failure. For example, if the number 123 appears, that means that the test failed on the 124th attempt. If ∞ appears, that means we optimistically canceled the rest of the trial because it was going so well.

In the tables below, `fatalii` is featured twice because I ran tests with VPN on and off. The host `thyme` is an older iMac (mid-2011, Intel® Core™ i5 2.70 GHz, macOS 10.13.6).


### 👵 Old Directory Service

When running the test case on the old service, `ldaps://edrn.jpl.nasa.gov`, in no case did tests fail. In fact, the tests had to be interrupted because they were running too well and there was no end in sight.

| Host          | At JPL | VPN | Trial 1 | Trial 2 | Trial 3 |
| ------------- | :----: | :-: | ------: | ------: | ------: |
| `edrn-labcas` |   ✓    | N/A |       ∞ |       ∞ |       ∞ |
| `mcl-labcas`  |   ✓    | N/A |       ∞ |       ∞ |       ∞ |
| `labcas-dev`  |   ✓    | N/A |       ∞ |       ∞ |       ∞ |
| `pds-dev`     |   ✓    | N/A |       ∞ |       ∞ |       ∞ |
| `pds-ipda`    |   ✓    | N/A |       ∞ |       ∞ |       ∞ |
| `tumor`       |   ✓    | N/A |       ∞ |       ∞ |       ∞ |
| `cancer`      |   ✓    | N/A |       ∞ |       ∞ |       ∞ |
| `fatalii`     |        |  ✓  |       ∞ |       ∞ |       ∞ |
| `fatalii`     |        |     |       ∞ |       ∞ |       ∞ |
| `thyme`       |        |     |       ∞ |       ∞ |       ∞ |


### 👶 New Directory Service

When running the test case on the new service, `ldaps://edrn-ds.jpl.nasa.gov`, tests failed quickly, but only on mainly on JPL hosts. Outside hosts fared much better. The number under the "trial" columns indicates the attempt number when trial failed.

| Host          | At JPL | VPN | Trial 1 | Trial 2 | Trial 3 |
| ------------- | :----: | :-: | ------: | ------: | ------: |
| `edrn-labcas` |   ✓    | N/A |      23 |      30 |      23 |
| `mcl-labcas`  |   ✓    | N/A |      23 |      53 |       4 |
| `labcas-dev`  |   ✓    | N/A |     111 |      79 |      77 |
| `pds-dev`     |   ✓    | N/A |      71 |     165 |     164 |
| `pds-ipda`    |   ✓    | N/A |       1 |       1 |       1 |
| `tumor`       |   ✓    | N/A |      39 |      62 |      32 |
| `cancer`      |   ✓    | N/A |       4 |       4 |       7 |
| `fatalii`     |        |  ✓  |     174 |    1039 |     561 |
| `fatalii`     |        |     |     393 |      38 |     336 |
| `thyme`       |        |     |       ∞ |       ∞ |       ∞ |


Note that `thyme` fared best with the new directory service. Is the fact it's an older, slower system a factor? Older Java? And why did `pds-ipda` only ever succeed just one time?

On 2020-09-02 Andrew Zimdars upgraded `ldaps://edrn-ds.jpl.nasa.gov` to Apache DS 2.0.0.M26. The updated test results are as follows:

| Host          | At JPL | VPN | Trial 1 | Trial 2 | Trial 3 |
| ------------- | :----: | :-: | ------: | ------: | ------: |
| `edrn-labcas` |   ✓    | N/A |       ∞ |       ∞ |       ∞ | 
| `mcl-labcas`  |   ✓    | N/A |       ∞ |       ∞ |       ∞ |
| `labcas-dev`  |   ✓    | N/A |       ∞ |       ∞ |       ∞ |
| `pds-dev`     |   ✓    | N/A |       ∞ |       ∞ |       ∞ |
| `pds-ipda`    |   ✓    | N/A |       ∞ |       ∞ |       ∞ |
| `tumor`       |   ✓    | N/A |       ∞ |       ∞ |       ∞ |
| `cancer`      |   ✓    | N/A |       ∞ |       ∞ |       ∞ |
| `fatalii`     |        |  ✓  |       ∞ |       ∞ |       ∞ |
| `fatalii`     |        |     |       ∞ |       ∞ |       ∞ |
| `thyme`       |        |     |       ∞ |       ∞ |       ∞ |

Also noteworthy: the "phoenix" watchdog didn't engage _once_ during these tests!

The [changelog](https://directory.apache.org/apacheds/news.html) for Apache DS doesn't say clearly if any of the addressed issues may have caused the marked improvement we see above; perhaps [DIRSERVER-2074](https://issues.apache.org/jira/browse/DIRSERVER-2074)? Perhaps [DIRSERVER-2145](https://issues.apache.org/jira/browse/DIRSERVER-2145)?


## 📝 Other Notes

This behavior seems to occur soley with Java-based clients to the new Directory Service. Python and OpenLDAP clients fare better.

There is a [watchdog program](https://github.com/EDRN/edrn.dir/blob/master/phoenix.sh) running on `edrn-ds.jpl.nasa.gov` that periodically checks the LDAP service and automatically restarts it if it fails to answer a simple query. This watchdog engaged *every single time* the "no stress" test was run.

The log file for the new Apache Directory Service routinely reports the following:
```
java.lang.OutOfMemoryError: Java heap space
    at java.nio.HeapByteBuffer.<init>(HeapByteBuffer.java:57)
    at java.nio.ByteBuffer.allocate(ByteBuffer.java:335)
    at org.apache.mina.core.buffer.SimpleBufferAllocator.allocateNioBuffer(SimpleBufferAllocator.java:42)
    at org.apache.mina.core.buffer.AbstractIoBuffer.capacity(AbstractIoBuffer.java:185)
    at org.apache.mina.filter.ssl.SslHandler.handshake(SslHandler.java:597)
    at org.apache.mina.filter.ssl.SslHandler.messageReceived(SslHandler.java:353)
    at org.apache.mina.filter.ssl.SslFilter.messageReceived(SslFilter.java:516)
    at org.apache.mina.core.filterchain.DefaultIoFilterChain.callNextMessageReceived(DefaultIoFilterChain.java:650)
    at org.apache.mina.core.filterchain.DefaultIoFilterChain.access$1300(DefaultIoFilterChain.java:49)
    at org.apache.mina.core.filterchain.DefaultIoFilterChain$EntryImpl$1.messageReceived(DefaultIoFilterChain.java:1141)
    at org.apache.mina.core.filterchain.IoFilterAdapter.messageReceived(IoFilterAdapter.java:122)
    at org.apache.mina.core.filterchain.DefaultIoFilterChain.callNextMessageReceived(DefaultIoFilterChain.java:650)
    at org.apache.mina.core.filterchain.DefaultIoFilterChain.fireMessageReceived(DefaultIoFilterChain.java:643)
    at org.apache.mina.core.polling.AbstractPollingIoProcessor.read(AbstractPollingIoProcessor.java:539)
    at org.apache.mina.core.polling.AbstractPollingIoProcessor.access$1200(AbstractPollingIoProcessor.java:68)
    at org.apache.mina.core.polling.AbstractPollingIoProcessor$Processor.process(AbstractPollingIoProcessor.java:1242)
    at org.apache.mina.core.polling.AbstractPollingIoProcessor$Processor.process(AbstractPollingIoProcessor.java:1231)
    at org.apache.mina.core.polling.AbstractPollingIoProcessor$Processor.run(AbstractPollingIoProcessor.java:683)
    at org.apache.mina.util.NamePreservingRunnable.run(NamePreservingRunnable.java:64)
    at java.util.concurrent.ThreadPoolExecutor.runWorker(ThreadPoolExecutor.java:1149)
    at java.util.concurrent.ThreadPoolExecutor$Worker.run(ThreadPoolExecutor.java:624)
    at java.lang.Thread.run(Thread.java:748)
```