import sys
from mininet.topo import Topo
from mininet.node import CPULimitedHost
from mininet.topo import Topo
from mininet.net import Mininet
from mininet.log import setLogLevel, info
from mininet.node import RemoteController
from mininet.cli import CLI
from mininet.link import TCLink



# What this script will doo 
# 1: ask you for the number of switches that you wouls like to have 
# 2: ask you for the number of hosts per switch that you would like to add 


# first ask for the number of of switches 

# now ask for the number of hosts per switch 

def intin():
	inp = 1 
	while(inp):
		isint =1
		try:
			num = int(input())
		except NameError:
			print "not a valid number please try again "
			isint =0
		except ValueError:
			print "not a valid number please try again "
			isint =0
		except SyntaxError:
			print "not a valid number please try again "
			isint =0
		if isint==1 and num >=1:
			inp =0
	return num


print "How many switches would you like to have in your network?:" 
nums = intin()
print "You will have " + str(nums) + " switches in your network\n" 

print "How many hosts would you like to have on each switch ?:"
numh = intin()
print "You Have selected " + str(numh) + " hosts per switch\n"

cons = nums - 1




class MyTopo( Topo ):
    def __init__( self ):
        # Initialize topology
        Topo.__init__( self )

		# Create host and Switch
        # Add link :: host to switch
        for s_num in range(1,nums+1):
                switch = self.addSwitch("s%s" %(s_num))
                for h_num in range(1,numh+1):
                        host = self.addHost("h%s" %(h_num + ((s_num - 1) * numh)))
                        self.addLink(host,switch)
 
        # Add link :: switch to switch
        for src in range(1,nums+1):
                for c_num in range(1,cons+1):
                        dst = src + c_num
                        if dst <= nums:
                                print("s%s" %src,"s%s" %dst)
                                self.addLink("s%s" %src,"s%s" %dst)
                        else:
                                dst = dst - nums
                                if src - dst > cons:
                                        print("s%s" %src,"s%s" %dst)
                                        self.addLink("s%s" %src,"s%s" %dst)


topos = { 'mytopo': ( lambda: MyTopo() ) }













