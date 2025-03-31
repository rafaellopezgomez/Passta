//This file was generated from (Academic) UPPAAL 5.1.0-beta5 (rev. C7C01B0740E14075), 2023-12-11

/*
F1.1: Probabilidad de que desde el estado INITIALIZING vaya a LISTENING con INIT_COMPLETE
*/
Pr ([][0,100000000]((attrs != 0 || event != 1) || ((attrs == 0 && event == 1) U[0,100000000] attrs == 1)))

/*
F2.1: Probabilidad de que desde el estado LISTENING vaya a UNCALIBRATED con el evento RS_SLAVE
*/
Pr ([][0,300000000]((attrs != 1 || event != 2) || ((attrs == 1 && event == 2) U[0,300000000] attrs == 2)))

/*
F3.1: Probabilidad de que desde el estado UNCALIBRATED vaya a SLAVE con el evento MASTER_CLOCK_SELECTED
*/
Pr ([][0,100000000]((attrs != 2 || event != 4) || ((attrs == 2 && event == 4) U[0,300000000] attrs == 3)))

/*
F4.1: Cuando un temporizador salta, hay una transici\u00f3n desde Listening a Listening
*/
Pr ([][0,100000000]((attrs != 1 || event != 3) || ((attrs == 1 && event == 3) U[0,300000000] (attrs == 1 && event != 3))))

/*
F4.2: Cuando un temporizador salta, hay una transici\u00f3n desde Uncalibrated a Listening
*/
Pr ([][0,100000000]((attrs != 2 || event != 3) || ((attrs == 2 && event == 3) U[0,300000000] attrs == 1)))

/*
F4.3: Cuando un temporizador salta, hay una transici\u00f3n desde Slave a Listening
*/
Pr ([][0,100000000]((attrs != 3 || event != 3) || ((attrs == 3 && event == 3) U[0,300000000] attrs == 1)))

/*
F8.1:
*/
Pr ([][0,900000000](attrs != 1 || \
(attrs == 1 U[0,900000000] \
((attrs == 2 && event == 2) || (attrs == 1 && event == 3))\
)\
)\
)

/*
F9.1: 
*/
Pr ([][0,900000000](attrs != 2 || \
(attrs == 2 U[0,900000000] \
((attrs == 3 && event == 4) || (attrs == 1 && event == 3))\
)\
)\
)

/*
F10.1: 
*/
Pr ([][0,900000000](attrs != 3 || \
(attrs == 3 U[0,900000000] \
(attrs == 1 && event == 3)\
)\
)\
)
