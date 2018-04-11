#!/usr/bin/python

from os import listdir, system;



print "Hello and welcome to the SDN cli";

# sudo mn --custom linetopo.py --topo mytopo --controller=remote,ip=10.0.2.2,port=6653 --switch user,protocols=OpenFlow13
runCom = "sudo mn --custom "; 
remoteC = "--controller=remote,ip=10.0.2.2,port=6653 ";
usersw = "--switch user,protocols=OpenFlow13";
dirFiles = listdir("./topos"); 
count = 0; 
for pyfile in dirFiles:
	print str(count) + ") "+ pyfile + "\n"; 
	count +=1; 

#First get the Topology we want to run 
getInput = 1; 
while(getInput):
	inpt = input("Please enter the # of the topology you would like to run: "); 
	if(inpt < len(dirFiles)):
		print "You selected : " + dirFiles[inpt];
		topo = "topos/" + dirFiles[inpt] + " --topo mytopo "		
		runCom += topo;
		getInput =0;
	else: 
		print "Your input was out of range please try again !"; 
	
#ask if the remote controller is wanted
inpt = raw_input("Would you like to use the remote controller? [Y/N] ");
stinp = str(inpt);
if(stinp.upper() == "Y"):
	runCom += remoteC
#Ask if the meter switch is wanted
inpt = raw_input("Would you like to use the ofswitch13 for use with meters? [Y/N] ");
stinp = str(inpt);
if(stinp.upper() == "Y"):
	runCom += usersw

print "The command being run is :" + runCom ;


system(runCom);


system("sudo mn -c");



print "\n\nGood bye, thank you for using the SDN cli !\n";









