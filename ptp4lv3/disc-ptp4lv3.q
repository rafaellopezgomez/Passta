//This file was generated from (Academic) UPPAAL 5.0.0 (rev. 714BA9DB36F49691), 2023-06-21

/*
F1: From Initializing state, ptp must transit to Listening on Initialization_complete event.
*/
Pr ([][0,900000000]((attrs != 0 || event != 1) || ((attrs == 0 && event == 1) U[0,100000000] attrs == 1)))

/*
F2: In Listening state, ptp must transit to Uncalibrated with BMC_slave event.
*/
Pr ([][0,900000000]((attrs != 1 || event != 2) || ((attrs == 1 && event == 2) U[0,300000000] attrs == 2)))

/*
F3: From Uncalibrated state, if the Master_clock_selected event happens there must be a transition to Slave.
*/
Pr ([][0,900000000]((attrs != 2 || event != 4) || ((attrs == 2 && event == 4) U[0,300000000] attrs == 4)))

/*
F5: If a fault is detected, there is a mandatory transition to Faulty state with the event Fault_detected iff ptp is not in the state Initializing.
*/
Pr ([][0,900000000](event != 3 || ((attrs != 0 && attrs != 3 && event == 3) U[0,300000000] attrs == 3)))\


/*
F6:  If ptp is in Faulty state, there must be a transition to Initializing with the event Fault_cleared once the fault is fixed.
*/
Pr ([][0,900000000]((attrs != 3 || event != 5) || ((attrs == 3 && event == 5) U[0,300000000] attrs == 0)))

/*
F7.1: When ptp4l is in Faulty state, it should remain in this state for no more than 16 seconds.
*/
Pr ([][0,900000000]((attrs != 3) || (attrs == 3 U[0,16000000] (attrs != 3))))

/*
F7.2: When ptp4l is in Faulty state, it should remain in this state for no more than 17 seconds.
*/
Pr ([][0,900000000]((attrs != 3) || (attrs == 3 U[0,17000000] (attrs != 3))))

/*
F7.3: When ptp4l is in Faulty state, it should remain in this state for no more than 18 seconds.
*/
Pr ([][0,900000000]((attrs != 3) || (attrs == 3 U[0,18000000] (attrs != 3))))

/*
F8:
The only three Listening outgoing transitions are: 
1) to Uncalibrated with BMC_slave event
2) to Listening with Announce_receipt_timeout_expires 
3) to Faulty with Fault_detected.
*/
Pr ([][0,900000000](attrs != 1 || \
(attrs == 1 U[0,900000000] \
((attrs == 2 && event == 2) || (attrs == 3 && event == 3))\
)\
)\
)

/*
F9:
The only three Uncalibrated outgoing transitions are: 
1) to Slave with Master_clock_selected event
2) to Listening with Announce_receipt_timeout_expires
3) to Faulty with Fault_detected.
*/
Pr ([][0,900000000](attrs != 2 || \
(attrs == 2 U[0,900000000] \
((attrs == 4 && event == 4) || (attrs == 3 && event == 3))\
)\
)\
)

/*
F10:

The only three Slave outgoing transitions are: 
1) to Uncalibrated with Synchronization_fault event
2) to Listening with Announce_receipt_timeout_expires and 
3) to Faulty with Fault_detected. 
*/
Pr ([][0,900000000](attrs != 4 || \
(attrs == 4 U[0,900000000] \
(attrs == 3 && event == 3)\
)\
)\
)

/*
F11:

The only two Faulty outgoing transitions are:
1) to Initializing with Fault_cleared event
2) to Faulty with Fault_detected.
*/
Pr ([][0,900000000](attrs != 3 || \
(attrs == 3 U[0,900000000] \
((attrs == 0 && event == 5) || (attrs == 3 && event == 3))\
)\
)\
)
