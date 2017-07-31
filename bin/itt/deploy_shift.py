import sys
import threading
from enum import Enum
import time

from kazoo.client import KazooClient
from kazoo.recipe.watchers import DataWatch
from kazoo.protocol.states import EventType


help_msg = "A script to deploy a shift operation on an existing overlay.\n" \
           "A Zookeeper server must be running and the path \'/itt/brokers\' must exist!\n" \
           "Requires Python 3.4 and Kazoo "

ZK_BROKERS = '/itt/brokers'
ZK_BROKER_STR = ZK_BROKERS + '/{}'
ZK_BROKER_OPS_STR = ZK_BROKER_STR + "/ops"
ZK_BROKER_STATS_STR = ZK_BROKER_STR + "/stats"
ZK_BROKER_OPS_STATUS_STR = ZK_BROKER_OPS_STR + "/status"

shift_cmd_template = "SHIFT#{}#{}#{}"

# mutex for output
lock = threading.Lock()
# broker status flags
broker_status = dict()


class OpStatus(Enum):
    Null = 'NONE'
    Started = 'STARTED'
    Finished = 'FINISHED'
    Error = 'ERROR'


def get_id(uri):
    pos = uri.rfind('/')
    if pos == -1 or not uri[pos+1:].isalnum():
        raise RuntimeError("Invalid URI: " + uri)
    return uri[pos+1:]


def all_done():
    for f in broker_status.values():
        if f is False:
            return False
    return True


def get_broker_op_data_watcher(uri):
    def broker_data_watcher(data, stat, event):
        with lock:
            if event and event.type == EventType.CHANGED:
                status = OpStatus(data.decode('utf-8').upper())
                print('*** znode data change on', uri, status)
                if status == OpStatus.Finished:
                    print('broker {} finished.'.format(uri))
                    broker_status[uri] = True
                    return False
                elif status == OpStatus.Error:
                    # broker could not do the op, mark op as error
                    print('broker {} failed.'.format(uri))
                    broker_status[uri] = True
                    # error or finish, no need for anymore callback
                    return False
                else:
                    # not finished and no error, wait for more callback
                    return True
            else:
                return True
    # return function for callback
    return broker_data_watcher


def main():
    if len(sys.argv) < 5:
        print(help_msg, "\n")
        print(sys.argv[0], 'zookeeper_server broker_uri_1 broker_uri_2 broker_uri_3')
        print('Example:', sys.argv[0], 'localhost:2181 socket://localhost:10001/broker1 '
              'socket://localhost:10002/broker2 socket://localhost:10003/broker3')
        exit()
    zk_server = sys.argv[1]
    broker_uris = sys.argv[2:5]
    shift_cmd = shift_cmd_template.format(*broker_uris)
    print('Deploying', shift_cmd)
    zk_client = KazooClient(zk_server, timeout=10 * 60)
    print('Connecting to Zookeeper at', zk_server)
    zk_client.start()
    for uri in broker_uris:
        broker_status[uri] = False
        bid = get_id(uri)
        # make sure broker is free
        data, stats = zk_client.get(ZK_BROKER_OPS_STATUS_STR.format(bid))
        op_status = OpStatus(data.decode('utf-8').upper())
        if op_status not in [OpStatus.Null, OpStatus.Finished]:
            raise RuntimeError('Cannot start {}, {} is in {} state'.format(shift_cmd, bid, op_status.name))
        # update broker's ops status
        zk_client.set(ZK_BROKER_OPS_STATUS_STR.format(bid), OpStatus.Null.value.encode('utf-8'))
        # write the cmd to the broker's ops
        zk_client.set(ZK_BROKER_OPS_STR.format(bid), shift_cmd.encode('utf-8'))
        # set watches for this broker's op status
        DataWatch(zk_client, ZK_BROKER_OPS_STATUS_STR.format(bid), func=get_broker_op_data_watcher(uri))
    print('Waiting for brokers ...')
    while not all_done():
        time.sleep(1)


if __name__ == '__main__':
    main()
