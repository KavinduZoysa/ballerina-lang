import ballerina/io;

public type Response object {

  function f1() returns string|error {
    string|error ret = "";
    if (ret is error) {
      io:println("ERROR");
    } else if (ret is string) {
      boolean x = true;
      if (x) {
        ret = "X";
      } else {
        ret = "Y";
      }
    }
  }

};
