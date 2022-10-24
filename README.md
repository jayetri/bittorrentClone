# BitTorrent P2P File Transfer Application

Project is implemented in Java using socket programming.

Instruction to run on each host:

1. Make sure the individual peer folders with the name peer_'peerID' is present in project folder 
1. naviate to project/src and run the command to compile the java files:
    javac p2p/*.java
1. Then java peerProcess peerID 

It is tested with below configuration:

**Common properties used by all peers are specified in the file Common.cfg as follows:**

NumberOfPreferredNeighbors 2

UnchokingInterval 5

OptimisticUnchokingInterval 15

FileName TheFile.dat

FileSize 10000232

PieceSize 32768

The meaningss of the first three properties can be understood by their names. The unit of of the first three properties can be understood by their names. The unit of UnchokingInterval and OptimisticUnchokingInterval is in secondUnchokingInterval and OptimisticUnchokingInterval is in secondss. . The The FileNaFileName property me property specifies the name of a file in which all peers are interested. FileSize specifies the size specifies the name of a file in which all peers are interested. FileSize specifies the size of the file in bytes. PieceSize specifies the size of a piece in bytes. In the above example, of the file in bytes. PieceSize specifies the size of a piece in bytes. In the above example, the file size is 10,000,232 bytes and the piece size is 32the file size is 10,000,232 bytes and the piece size is 32,768 bytes. Then the number of ,768 bytes. Then the number of pieces of this file is 306. Note that the size of the last piece is only 5,992 bytes. Note pieces of this file is 306. Note that the size of the last piece is only 5,992 bytes. Note that the file that the file Common.cfgCommon.cfg serves like serves like the the metainfo file in BitTorrent. Whenever a peer metainfo file in BitTorrent. Whenever a peer starts, it should read the file starts, it should read the file Common.cfgCommon.cfg and sand set up et up the the corresponding variables.corresponding variables.

**The peer information is specified in the file PeerInfo.cfg in the following format:**

[peer ID] [host name] [listening port] [has file or not]

[peer ID] [host name] [listening port] [has file or not]

The following is an example of file PeerInfo.cfg


1001 host1 6008 1

1002 host2 6008 0

1003 host3 6008 0

1004 host4 6008 0

1005 host5 6008 0

1006 host6 6008 1

1007 host7 6008 0

1008 host8 6008 0

1009 host9 6008 0

Each line in file PeerInfo.cfg represents a peer. The first column is the peer ID, which is which is a positive integer number. The second column is the host name. The third column is the port number at which the peer listens.The port numbers of the the peers peers may be different from each other. The fourth column specifies whether it has the file or not. We only have two options here. ‘1’ means that the peer has the complete file and ‘0’ means that the peer does not have the file. We do not consider the case where a peer has only some pieces of the file. Note, however, that more than one peer may have the file. PeerInfo.cfg serves like a tracker in BitTorrent.
