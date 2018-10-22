# RemotePort

前提:
一台在公网上的服务器
一台能够连接服务的本地主机

服务器：
调用new Creature server = new Creature(4959)，在端口4959等待主机连接

主机：
调用Server server = new Server(CREATURE_HOST, 4959, 8080)，连接服务器CREATURE_HOST的4959端口，用于转发本地8080端口的流量
调用server.bind(80)，绑定服务器80端口，其他客户机连接服务器80端口都会被反向代理到本地主机的端口8080
