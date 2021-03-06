INTERFACE ITF END_INTERFACE
CLASS A IMPLEMENTS ITF END_CLASS
CLASS B EXTENDS A END_CLASS
CLASS C EXTENDS A END_CLASS

PROGRAM MY_PROGRAM
  VAR
    a : A;
    b : B;
    i : ITF;
    r : REF_TO A;
  END_VAR

    VAR_INPUT
      switch : BOOL;
    END_VAR

  IF (switch) THEN
    i := a;
    r := REF(a);
  ELSE
    i := b;
    r := REF(b);
  END_IF
END_PROGRAM