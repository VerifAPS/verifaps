PROGRAM abc
    VAR
        s : INT;
    END_VAR

    VAR_INPUT
        i : INT;
    END_VAR

    VAR_OUTPUT
        o : INT;
    END_VAR


    ACTION up
        s := (s + i);
    END_ACTION
    ACTION down
        s := (s - i);
    END_ACTION

    IF (i < INT#0) THEN
        down();
    ELSE
        up();
    END_IF
    o := s;
END_PROGRAM
