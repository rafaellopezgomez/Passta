//This file was generated from (Academic) UPPAAL 5.0.0 (rev. 714BA9DB36F49691), 2023-06-21

/*
F1: From Initializing state, ptp must transit to Listening on Initialization_complete event.
*/
Pr ([][0,100000000]((attrs != 0 || event != 1) || ((attrs == 0 && event == 1) U[0,100000000] attrs == 1)))

/*
F2: In Listening state, ptp must transit to Uncalibrated with BMC_slave event.
*/
Pr ([][0,300000000]((attrs != 1 || event != 2) || ((attrs == 1 && event == 2) U[0,300000000] attrs == 2)))

/*
F3: From Uncalibrated state, if the Master_clock_selected event happens there must be a transition to Slave
*/
Pr ([][0,100000000]((attrs != 2 || event != 4) || ((attrs == 2 && event == 4) U[0,300000000] attrs == 3)))

/*
F4.1: When a timer expires, ptp must transit from state Listening to Listening.
*/
Pr ([][0,100000000]((attrs != 1 || event != 3) || ((attrs == 1 && event == 3) U[0,300000000] (attrs == 1 && event != 3))))

/*
F4.2: When a timer expires, ptp must transit from state Uncalibrated to Listening.
*/
Pr ([][0,100000000]((attrs != 2 || event != 3) || ((attrs == 2 && event == 3) U[0,300000000] attrs == 1)))

/*
F4.3: When a timer expires, ptp must transit from state Slave to Listening.
*/
Pr ([][0,100000000]((attrs != 3 || event != 3) || ((attrs == 3 && event == 3) U[0,300000000] attrs == 1)))

/*
F8:
The only three Listening outgoing transitions are: 
1) to Uncalibrated with BMC_slave event
2) to Listening with Announce_receipt_timeout_expires 
3) to Faulty with Fault_detected.
*/
Pr ([][0,900000000](attrs != 1 || \
(attrs == 1 U[0,900000000] \
((attrs == 2 && event == 2) || (attrs == 1 && event == 3))\
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
((attrs == 3 && event == 4) || (attrs == 1 && event == 3))\
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
Pr ([][0,900000000](attrs != 3 || \
(attrs == 3 U[0,900000000] \
(attrs == 1 && event == 3)\
)\
)\
)
