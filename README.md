# ChunkPreGenerator

One of the most resource consuming task is probably 'chunk generation,' and it is therefore adviced for any server to pre-generate the chunks and load them to memory only when they are needed. Imagine 20 or more players moving around exploring new chunks every tikcs. It will cause a massive chunk generation overhead, which will degrade the performance of server.

## Problem

However, pre-generating the chunk isn't always easy because when server is running, it's not easy to generate the world and synchronized entities in it. **WorldBoarder** plugin, for example, tries to pre-generate the chunks in the server thread without dropping the server performance, yet it significantly takes more time to generate compared to how we can generated chunks when we are dealing with the spawn area.

## Solution

Well, simply, just treat the entire region as a giant 'spawn area,' and let the vanilaa chunk generator to deal with it. In this way, the chunk generator is not bounded to various synchronization tasks and focus only on generating chunks.

## Usage

Place the .jar file together with spigot.jar (currently only tested for 1.16.1), and execute it using

1. `java -jar ChunkPreGenerator-1.0-SNAPSHOT.jar`

Or you may just double click it if that's how you usually execute .jar files.

You will prompted to enter 'world name' and 'max size,' where world name is world name, and max size is radius to cover. Radius of 1000 will cover the area from (-1000, -1000) to (1000, 1000) in a square.

2. Alternatively, `java -jar ChunkPreGenerator-1.0-SNAPSHOT.jar worldname maxsize` to bypass the prompts.

## Finally

This is very rough application, which means it merely mimics how CraftSpigot deals with the NMS chunk generation, so unexpected things can happen. As far as my knowledge, it's working without problem. It also generates other unncessary files, such as banned-ips.json, banned-players.json, etc. which you usually see in a regular Spigot/Paper server, yet these are just empty files, so do not use them unless that's what you want to do. All you need is the `worldname/region/r.x.x.mca` files, which are the pregenerated chunks.
