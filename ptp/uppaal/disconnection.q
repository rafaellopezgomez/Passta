//This file was generated from (Academic) UPPAAL 5.1.0-beta5 (rev. C7C01B0740E14075), 2023-12-11

/*
F1.1: Probabilidad de que desde el estado INITIALIZING vaya a LISTENING con INIT_COMPLETE
*/
Pr ([][0,900000000]((attrs != 0 || event != 1) || ((attrs == 0 && event == 1) U[0,100000000] attrs == 1)))

/*
F2.1: Probabilidad de que desde el estado LISTENING vaya a UNCALIBRATED con el evento RS_SLAVE
*/
Pr ([][0,900000000]((attrs != 1 || event != 2) || ((attrs == 1 && event == 2) U[0,300000000] attrs == 2)))

/*
F3.1: Probabilidad de que desde el estado UNCALIBRATED vaya a SLAVE con el evento MASTER_CLOCK_SELECTED
*/
Pr ([][0,900000000]((attrs != 2 || event != 4) || ((attrs == 2 && event == 4) U[0,300000000] attrs == 4)))

/*
F5.1: Probabilidad de que si ocurre el evento FAULT_DETECTED vaya al estado FAULTY
*/
Pr ([][0,900000000](event != 5 || ((attrs != 0 && attrs != 5 && event == 5) U[0,300000000] attrs == 5)))\


/*
F6.1: Probabilidad de que desde FAULTY vaya al estado INITIALIZING con el evento FAULT_CLEARED cuando el fallo se soluciona
*/
Pr ([][0,900000000]((attrs != 5 || event != 3) || ((attrs == 5 && event == 3) U[0,300000000] attrs == 0)))

/*
F7.1: Probabilidad que ya no est\u00e9 en Faulty despu\u00e9s de 16 segundos
*/
Pr ([][0,900000000]((attrs != 5) || (attrs == 5 U[0,16000000] (attrs != 5))))

/*
F7.2: Probabilidad que ya no est\u00e9 en Faulty despu\u00e9s de 17 segundos
*/
Pr ([][0,900000000]((attrs != 5) || (attrs == 5 U[0,17000000] (attrs != 5))))

/*
F7.3: Probabilidad que ya no est\u00e9 en Faulty despu\u00e9s de 17 segundos
*/
Pr ([][0,900000000]((attrs != 5) || (attrs == 5 U[0,18000000] (attrs != 5))))

/*
F8.1:
*/
Pr ([][0,900000000](attrs != 1 || \
(attrs == 1 U[0,900000000] \
((attrs == 2 && event == 2) || (attrs == 5 && event == 5))\
)\
)\
)

/*
F9.1: 
*/
Pr ([][0,900000000](attrs != 2 || \
(attrs == 2 U[0,900000000] \
((attrs == 4 && event == 4) || (attrs == 5 && event == 5))\
)\
)\
)

/*
F10.1: 
*/
Pr ([][0,900000000](attrs != 4 || \
(attrs == 4 U[0,900000000] \
(attrs == 5 && event == 5)\
)\
)\
)

/*
F11.1
*/
Pr ([][0,900000000](attrs != 2 || \
(attrs == 2 U[0,900000000] \
((attrs == 0 && event == 3) || (attrs == 5 && event == 5))\
)\
)\
)
