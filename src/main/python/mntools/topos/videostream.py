import sys 

from mininet.topo import Topo
from mininet.node import CPULimitedHost
from mininet.topo import Topo
from mininet.net import Mininet
from mininet.log import setLogLevel, info
from mininet.node import RemoteController
from mininet.cli import CLI
from mininet.link import TCLink





# what I need to do 
# 1: want to create a mininet topology that is set but can vary the speeda within 
#		     S4  
#	     	  L6 	L5
#	H1 L1  S1          S3 L2 H2
#		  L3    L4
# 		     S2 
#



L1 = 25 ;
L2 = 25 ;
L3 = 25 ;
L4 = 25 ;
L5 = 25 ;
L6 = 25 ;
L7 = 25 ;

D1 = "1ms";
D2 = "1ms";
D3 = "1ms";
D4 = "1ms";
D5 = "1ms";
D6 = "1ms";
D7 = "1ms";


class MyTopo( Topo ):
    "Simple topology example."

    def __init__( self ):
        "Create custom topo."
        # Initialize topology
        Topo.__init__( self )

	# Add hosts here 
	h1 = self.addHost( 'h1' )
	h2 = self.addHost( 'h2' )
	h3 = self.addHost( 'h3' )
	# Add switches here 
	s1 = self.addSwitch( 's1' )
	s2 = self.addSwitch( 's2' )
	s3 = self.addSwitch( 's3' )
	s4 = self.addSwitch( 's4' )

        # Add links between host and switches 
	self.addLink( h1, s1, bw=L1, delay=D1, loss=0)
	self.addLink( h2, s3, bw=L2, delay=D2, loss=0)
	self.addLink( h3, s2, bw=L2, delay=D7, loss=50)

	# Add the links between switches
	self.addLink( s1, s2 ,bw=L3, delay=D3)		#link 1
	self.addLink( s2, s3, bw=L4, delay=D4)		#link 2
	self.addLink( s3, s4 ,bw=L5, delay=D5)		#link 3
	self.addLink( s4, s1 ,bw=L6, delay=D6)		#link 4






topos = { 'mytopo': ( lambda: MyTopo() ) }

