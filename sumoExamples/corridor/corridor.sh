#!/bin/bash

sumo-gui -n corridor.net.xml -r corridor.rou.xml --summary-output output_sumo.corridor.xml --remote-port 8000
