#!/bin/bash

sumo-gui -n manhattan.net.xml -r manhattan.rou.xml --summary-output output_sumo.manhattan.xml --remote-port 8000
