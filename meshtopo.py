
#my mesh topology

from mininet.topo import Topo

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
	h4 = self.addHost( 'h4' )
	# Add switches here 
	s1 = self.addSwitch( 's1' )
	s2 = self.addSwitch( 's2' )
	s3 = self.addSwitch( 's3' )
	s4 = self.addSwitch( 's4' )

        # Add links between host and switches 
	self.addLink( h1, s1)
	self.addLink( h2, s2)
	self.addLink( h3, s3)
	self.addLink( h4, s4)

	# Add the links between switches
	self.addLink( s1, s2, bw=150)
	self.addLink( s1, s3, bw=150)
	self.addLink( s1, s4, bw=150)
	self.addLink( s2, s3, bw=150)
	self.addLink( s2, s4, bw=150)
	self.addLink( s3, s4, bw=150)



topos = { 'mytopo': ( lambda: MyTopo() ) }

