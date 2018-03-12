#!/usr/bin/python

from mininet.net import Mininet
from mininet.node import RemoteController, OVSSwitch
from mininet.cli import CLI
from mininet.log import setLogLevel, info
from mininet.link import TCLink

def emptyNet():

    net = Mininet(topo=None, build=False, link=TCLink)

    info( '*** Adding controller\n' )
    net.addController('c0', controller=RemoteController,ip="127.0.0.1",port=6653)

    info( '*** Adding hosts\n' )
    h1 = net.addHost('h1')
    h2 = net.addHost('h2')
    h3 = net.addHost('h3')
    h4 = net.addHost('h4')
    h5 = net.addHost('h5')
    h6 = net.addHost('h6')
    h7 = net.addHost('h7')
    h8 = net.addHost('h8')

    info( '*** Adding switch\n' )
    s1 = net.addSwitch('s1', cls=OVSSwitch)
    s2 = net.addSwitch('s2', cls=OVSSwitch)
    s3 = net.addSwitch('s3', cls=OVSSwitch)
    s4 = net.addSwitch('s4', cls=OVSSwitch)
    s5 = net.addSwitch('s5', cls=OVSSwitch)
    s6 = net.addSwitch('s6', cls=OVSSwitch)
    s7 = net.addSwitch('s7', cls=OVSSwitch)

    info( '*** Creating links\n' )

    ## host - switch
    net.addLink(h1, s3)
    net.addLink(h2, s3)
    net.addLink(h3, s4)
    net.addLink(h4, s4)
    net.addLink(h5, s6)
    net.addLink(h6, s6)
    net.addLink(h7, s7)
    net.addLink(h8, s7)

    ## switches
    net.addLink(s1, s2)
    net.addLink(s2, s3)
    net.addLink(s2, s4)
    net.addLink(s3, s4)
    net.addLink(s1, s5)
    net.addLink(s5, s7)
    net.addLink(s5, s6)
    net.addLink(s6, s7)

    info( '*** Starting network\n')
    net.start()

    info( '*** Running CLI\n' )
    CLI(net)

    info( '*** Stopping network' )
    net.stop()

if __name__ == '__main__':
    setLogLevel( 'info' )
    emptyNet()
