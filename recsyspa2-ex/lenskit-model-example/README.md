This is an example program that shows how to build a LensKit algorithm and evaluate it.

## LensKit Algorithm Architecture

A typical LensKit algorithm consts of at least three pieces:

1.  An item scorer.  This does the interesting work, producing user-personalized scores for items.
2.  A model.  Most algorithms will have some kind of model that is pre-computed; we generally
    represent this with a dedicated class that stores the relevant model data and is serializable.
3.  A model builder.  This is a separate class that builds the model.  It implements the Java
    injection Provider interface.

## Extended Item-User Mean

This code includes an implementation of a complete algorithm that scores items using a damped
user-item mean.  That is, it scores using mu + bi + bu, where mu is the global mean, bi is the
item's average offset from the mean rating, and bu the user's average offset from item mean.