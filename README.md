# NFCMessageBoard

NFC Tag message board is an android app that is currently a proof of concept of a localized message system. 
( But it could also be useful as a personal reminder system )
It is quite simple, and does the job of reading and writing to any plain text NFC tags. At a minimum you are recommended to use NTAG216 that gives you approximately 800 bytes of writeable memory.
It also has the advantage of being relatively private means of communicating (even if it runs the issue of spoofing), since it is essentially a dead drop system.

##Potential Uses:

* Geo Cache GuestBook
* Ingress Local Message Store
* Personal Reminder System (E.g. place a tag on your desk, and tap to read the latest reminder)

##Features:

* Reads Plain Text
* Write and prepends a message to a Plain Text Tag
* UTF-8 support, so can show emoticon.
* Links, email, map address are autolinked

## Wishlist:

* Cleaner and less hacky sourcecode
* Saves tag content based on tag UUID, so you can follow a conversation in the tag by tapping over a period of time. (It appends any new content extracted from the tag to the saved tag content system)
 - E.g. allows you to scan, and then read the conversation away from the tag.
* Parse messages to display nicer, and allow for extra metadata (e.g.Reply to field)
* Allow messages to hop to another location from another phone (This will be useful for larger tags)
 - Eventally, would be nice to a sneakernet BBS. Where every tag will have a portion dedicated to autoswapping foreign messages/threads
* For larger tags, could also allow for threads, allowing for a more structered conversation. 
* Uploads latest messages to a tracking website (Might be a seperate app, to let people avoid the internet enabled version)

## How to contribute:

Clone this repo, and push your changes to me. It is more likely to be accepted, if it cleans up the codes and makes it easier to read... or it is one of the wishlist entires.

Source: https://github.com/mofosyne/NFCMessageBoard

License: GPL (in COPYING.txt https://github.com/mofosyne/NFCMessageBoard/blob/master/COPYING.txt )

Issue Tracking: https://github.com/mofosyne/NFCMessageBoard/issues

ReadMe: https://github.com/mofosyne/NFCMessageBoard/blob/master/README.md

Google Play Appstore Entry: https://play.google.com/store/apps/details?id=com.briankhuu.nfcmessageboard



# Why an app?
Obviously this can be done by any tag writing app. But the main objective of this, is to make it easy to use as a guestbook.
If your message cannot fit into the tag, it will discard the older messages. So you should ideally keep you message short.
Also having a bigger tag is ideal. E.g. 1kb tag is the minimum size you need for a useful Message Board tag.

# Note:

It doesn't work on empty tags, you need to at least seed it with a plain text NFC record (even if it is a single space.)

##Making your own Geo Cache? You might want some artworks to make your tag easier to recognize as a Message Board enabled tag.

* Credit Card Size (85.60×53.98 mm) (Contains information about installing this app):
 - http://i.imgur.com/KLFgqL7.jpg

* Stickers (Good for round NFC tags):
 - http://i.imgur.com/0zbssBd.png


