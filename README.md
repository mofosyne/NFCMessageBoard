# NFCMessageBoard
NFC Tag messageboard is an android app that is currently a proof of concept of a localized deaddrop message system.

It simply works by reading a plain text file NFC tag, and appending the latest message to it.

Obviously this can be done by any tag writing app. But the main objective of this, is to make it easy to use as a guestbook.

If your message cannot fit into the tag, it will discard the older messages. So you should ideally keep you message short.

Also having a bigger tag is ideal. E.g. 1kb tag is the minimum size you need for a useful Message Board tag.

# Note

It doesn't work on empty tags, you need to at least seed it with a plain text NFC record (even if it is a single space.)
