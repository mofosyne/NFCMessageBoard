# NFCMessageBoard

NFC Tag message board is an android app that is currently a proof of concept of a localized message system. 
( But it could also be useful as a personal reminder system )

It is quite simple, and does the job of reading and writing to any plain text NFC tags. At a minimum you are recommended to use NTAG216 that gives you approximately 800 bytes of writeable memory.

It also has the advantage of being relatively private means of communicating (even if it runs the issue of spoofing), since it is essentially a dead drop system.

There is even an entry on it in n-o-d-e net magazine: http://n-o-d-e.net/post/117867873191/digital-dead-drops-secret-communication-in-plain

## Potential Uses:

- Geo Cache GuestBook
- Ingress Local Message Store
- Personal Reminder System (E.g. place a tag on your desk, and tap to read the latest reminder)

## Features:

- Reads Plain Text
- Write and prepends a message to a Plain Text Tag
- Message can be either a message, or nickname, or a timestamp only; or a combination of each one.
- UTF-8 support, so can show emoticon.
- Links, email, map address are autolinked
- Static Header Text - So you can keep a short unchanging message on the first line.
- Create a new tag - Easier to get started with this! Just get an empty writable NFC tag!
- Message text are selectable
- You can bold text via markdown style `**bold**`

## Wishlist:

- Cleaner and less hacky sourcecode
- Saves tag content based on tag UUID, so you can follow a conversation in the tag by tapping over a period of time. (It appends any new content extracted from the tag to the saved tag content system)
 - E.g. allows you to scan, and then read the conversation away from the tag.
- Parse messages to display nicer, and allow for extra metadata (e.g.Reply to field)
- Allow messages to hop to another location from another phone (This will be useful for larger tags)
 - Eventually, would be nice to a sneakernet BBS. Where every tag will have a portion dedicated to autoswapping foreign messages/threads
- For larger tags, could also allow for threads, allowing for a more structured conversation.
- Uploads latest messages to a tracking website (Might be a separate app, to let people avoid the internet enabled version)

## Note:

- NFC tags placed outdoor should be of the waterproof/epoxy variety. Otherwise the rain will shortcircuit/kill the onboard NFC chip.
- NFC tags placed on metal needs to be of the variety that supports sticking on metal. The metal would usually interfere with standard NFC stickers.
- Sometimes this app accidentally wipes the tag. A restore button is included just in case.

## Markdownish Syntax:

This is the supported syntax. Can't support full markdown, since this is only a lite hacky implementation. But kind handy for a no frills display that is easy to type on a mobile phone keypad.

```
# H1 only in first line (Due to technical hacks used)

## H2 headers as usual

## Styling
Like: *italic* **bold** ***bold_italic***

## Classic List
 - list item 1
 - list item 2

## Nonstandard List Syntax
- list item 1
- list item 2

## Block Quotes
> Quoted stuff

## codes
here is inline `literal` codes. Must have space around it.
    
    ```
    codeblocks
    Good for ascii art
    ```
    
        Or 4 space code indent like classic markdown. 
    
```

## How to contribute:

Clone this repo, and push your changes to me. It is more likely to be accepted, if it cleans up the codes and makes it easier to read... or it is one of the wishlist entries.

Source: https://github.com/mofosyne/NFCMessageBoard

License: GPL (in COPYING.txt https://github.com/mofosyne/NFCMessageBoard/blob/master/COPYING.txt )

Issue Tracking: https://github.com/mofosyne/NFCMessageBoard/issues

ReadMe: https://github.com/mofosyne/NFCMessageBoard/blob/master/README.md

Download App:

- Android Marketplace: https://play.google.com/store/apps/details?id=com.briankhuu.nfcmessageboard

- F-Droid Marketplace: https://f-droid.org/repository/browse/?fdfilter=nfc&fdid=com.briankhuu.nfcmessageboard

# Why an app?
Obviously this can be done by any tag writing app. But the main objective of this, is to make it easy to use as a guestbook.
If your message cannot fit into the tag, it will discard the older messages. So you should ideally keep you message short.
Also having a bigger tag is ideal. E.g. 1kb tag is the minimum size you need for a useful Message Board tag.

# Note:

It doesn't work on empty tags, you need to at least seed it with a plain text NFC record (even if it is a single space.)

## Making your own Geo Cache? You might want some artworks to make your tag easier to recognize as a Message Board enabled tag.

- Stickers (Good for round NFC tags):
 - http://i.imgur.com/0zbssBd.png

- Credit Card Size (85.60�53.98 mm) (Contains information about installing this app):
 - http://i.imgur.com/KLFgqL7.jpg


# keywords:

deaddrop, dead drop, deaddrops, dead drops, nfc message board


-----


# version notes

## V2.3.4
Adjusted minimum target to gingerbread V2.3.3 , since there are still phones out there that uses it. Might as well see if they can use it, and not to leave them missing out on the fun.

## V2.3.3
Fixed the header title not appearing on tag creation, a crash bug, and also added a readme section.

## V2.3.1
 Basically had to squash a bug within the "psudo markdown" rendering which made it stuck in an infinite loop when reading a tag that lacks a particular symbol.




----------

# For android coders

Oh, btw, did you know that this readme file is auto copied from the root directory of the project folder, into this app's `/src/main/res/raw` folder before being compiled?

It's quite a handy way to keep your readme file accessible in the resource directory (e.g. so you can view the readme file from within your app!)

Here is how to do it. Copy the code below to your gradle buildfile in /app/src/build.gradle . (You might need to adjust the "from" settings, but should be self explanatory).

     /*
     * Based on https://discuss.gradle.org/t/how-to-copy-and-rename-a-single-file/5956
     * */
     task copyreadme(type: Copy) {
         from '../README.md'
         into 'src/main/res/raw'
         rename { String fileName ->
             fileName.replace("README.md", "readme.md")
         }
     }
     tasks.copyreadme.execute()

Once you do that, everytime you build your file. It will auto copy the readme file to the raw resource directory of your android project folder. Let me know if it was handy!