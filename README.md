# yandex-mail-terminal
Application for accessing Yandex mail. Works by sending GET and POST requests to the website using Apache's library. While the application works fine at the time of writing, changes to the website may cause it to stop working in the future. Also, login data must be sent in the form of strings, which can compromise security of the account if a memory dump is performed. Unfortunately, this is an inherent flaw in the Java language and nothing can be done to solve it. It is recommended not to use this for accessing your mail account on a shared computer. 

Currently, it can only view mail. Will try to add more functionality if I get the time. Line breaks are completely ignored by Jsoup and the mail content needs to be formatted for console.

(Yes, I am aware that using the API is far more efficient than downloading useless HTML data. This project was just meant to be a learning exercise.)
