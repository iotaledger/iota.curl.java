
Java Porting of IOTA's Proof of Work function, "CURL".

CURL technically is a sponge function (https://en.wikipedia.org/wiki/Sponge_function)

To execute:

	final IotaCurlMiner iotacurl = new IotaCurlMiner();
    final String mined = iotacurl.doCurlPowSingleThread(in, difficulty);
	System.err.println("Mined: " + mined);

where "in" is a 2673 String character lenght composed by [A-Z] and '9'.

To compile:
	
	mvn clean compile

To test:

	mvn test

IOTA: www.iotatoken.com

	
	
