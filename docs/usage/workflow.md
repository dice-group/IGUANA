# Workflow

Iguana will first parse the configuration file. 
Afterwards it will execute each task for each connection for each dataset. 

Imagine it like the following:

* for each dataset D
    * for each connection C
        * for each task T
            1. execute pre script hook
            2. execute task T(D, C)
            3. collect and calculate results
            4. write results
            5. execute post script hook
