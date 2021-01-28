# HanukCoin


## Explanation
1. This is an improvement to a project that was given as an assignment to imitate a blockchain network as a part of studies at [_Odyssey_](https://www.madaney.net/en/site/programs/odyssey/).
2. The project includes multiple interesting features in the programming world:
   1. Parallel Computing
   2. Networking Protocols
   3. Block-Chain
3. The project's flow and hierarchy include:
   1. Active nodes list class and auxiliary class
   2. Server side classes
   3. Network protocols classes



## The Project Specification

### Network Messages Format

#### Request/Response Format

| Field's Name  | Size In Bytes   | Format           | #Repetitions  | Comment                       |
|--------------|-----------------|------------------|--------------|-------------------------------|
| cmd          |               4 | int32 big-endian |              | value request=1 or response=2 |
| start_nodes  |               4 | int32 big-endian |              | always 0xBeefBeef             |
| nodes_count  |               4 | int32 big-endian |              | can be zero or more           |
| nodes        | 8 bytes or more | see Node below   | nodes_count  |                               |
| start_blocks |               4 | int32 big-endian |              | always 0xDeadDead             |
| blocks_count |               4 | int32 big-endian |              | can be zero or more           |
| blocks       |              36 | see Block below  | blocks_count |                               |

#### Node Format

| Field's Name | Size In Bytes | Format                          | #Repetitions | Comment                       |
|--------------|---------------|---------------------------------|-------------|-------------------------------|
| cmd          |             4 | int32 big-endian                |             | value request=1 or response=2 |
| name_len     |             1 | int8                            |             |                               |
| name         |             1 | char ASCII                      | name_len    | team-name                     |
| host_len     |             1 | int8                            |             |                               |
| host         |             1 | char ASCII                      | host_len    |                               |
| port         |             2 | int16 big-endian                |             |                               |
| last_seen_ts |             4 | int32 big-endian unix timestamp |             |                               |

##### note:
The last_seen_ts field contains the time this node was last seen. The one who fills it is the server itself about itself. Each node (server) that unifies node lists in the present should take the most recent (maximum) time between the two lists - the one it has received, and the one it has in its memory. Node which was last visible more than half an hour ago should be deleted from the node list.

#### Block Format

| Field's Name  | Size In Bytes | Format                          | Comment                                                            |
|---------------|---------------|---------------------------------|--------------------------------------------------------------------|
| cmd           |             4 | int32 big-endian                | value request=1 or response=2                                      |
| serial_number |             4 | int32 big-endian                | must be +1 from previous block                                     |
| wallet        |             4 | int32 big-endian                | Wallet that won a coin                                             |
| prev_sig      |             8 | byte [8]                        | the first half of MD5 (previous_block)                              |
| puzzle        |             8 | byte [8]                        | 8 bytes that make the MD5 of this block end with zeros as required |
| sig           |            12 | byte [12]                       | MD5 signature of this block - first 12 bytes out 16.               |
| last_seen_ts  |             4 | int32 big-endian unix timestamp |                                                                    |

### Messages Exchange
Each node will send a request to 3 random nodes if one of the following occurs:
1. A change has occurred in the node's node list.
2. A change has occurred in the node's blockchain.
3. 5 minutes have passed since the last broadcast message.

### Validation of Messages
1. The node should merge the incoming node list with the one in its memory according to the rules mentioned above.
2. Each time a node receives a list, it has to go through and check the correctness of the list of blocks received.
   1. If the resulting block list is incorrect - it should be ignored.
   2. If the list of blocks received is longer than what should be served in memory - keep the longer one and save it to disk.
   3. If the list of blocks is exactly the same length - take the one in which the puzzle field in the last block is the smallest (small lexically when looking at the bytes from beginning to end).

      (This is because if two groups were to mine coins at the same time, and their lists are the same length - neither of them will continue to mine due to the limitation on two blocks with the same wallet (will be explained further on) and also neither of them holds a longer list then unconditionally the network can get stuck).
3. In case of restarting the server - the list must be uploaded from the disk, and a validation must be performed on it.
   How to check the integrity of the list of blocks:
   1. The first block - is numbered 0 and must be identical to the "Genesis block".
   2. The serial_number number of each block is 1 by its predecessor.
   3. For each block the MD5 signature must be calculated on each block except the sig field. Regarding the signature, check the following two things:
      1. The signature matches the sig field.
      2. The signature ends in a sequence of zeros of NZ length or more. The number NZ depends on the serial number of the block.

         NZ = 20 + NumBits (serial_number)); NumBits (k) = 1 + floor (Log2 (k))
   4. The wallet number in this block is not the same as the wallet number in the previous block. This condition means that no wallet can win more than half the number of coins.



## Authors

* [**Itay Peleg**](https://github.com/Itayo252)

* [**Dvir Golan**](https://github.com/dvirgol10)

* [**Yuval Nosovitsky**](https://github.com/yuval12311)

