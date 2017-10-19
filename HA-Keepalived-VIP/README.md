# VIP Configuration by using Docker image HA-Keepalived
# Thanks to Zhang Chunliang
Steps:
docker run -itd --restart=always --name=Keepalived --net=host --cap-add=NET_ADMIN -e VIRTUAL_IP=192.168.122.100 -e CHECK_PORT=22 -e VRID=151 -e INTERFACE=wlp1s0 -e NETMASK_BIT=24-e haproxy-keepalived:1.0 
check_port是实际侦听端口， interface是加载虚拟ip的网卡，这个根据实际情况改下，其它不用改了
VIRTUAL_IP肯定改成实际了
