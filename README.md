Try implement postgresql tool from this article 
https://blog.codinghorror.com/get-your-database-under-version-control/

TODO:
Function drop works incorrectly, if there is installed extension in schema with user-defines functions.
(We should drop ONLY user-defined function, but now i DROP ALL, that cause DB broken :3 )

Need to determine, how detect extension stored procedures and user procedures..
https://stackoverflow.com/questions/25387168/how-to-find-all-user-defined-not-extension-related-functions-in-postgresql
