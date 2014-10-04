plotsPlugin
===========

Plots plugin developed for the Arcane Survival server.

Author: "Morios" Mark Talrey

Current Version: RC.3.1.5 for Minecraft 1.7.10

Notes:
- Plot data is saved to "plots.ser" in the main server folder. This is serialized object data created by Java, and may not be human-readable.
- Credit data is saved to "plotBank.ser" in the main server folder. Same warning applies.
- Currently saves on reload or server stop. I may add timed saves in at a later date.
- Credits should be working now, there's a bank implemented!

Commands:
- credits: (alias: /cr) See your balance or send credits to another player (/credits give <name> #)
- plot: master command for plots. Defaults to 'list' option.
- plot buy: creates a plot. Can create using corners or a radius. Defaults to radius.
- plot sell: Removes a plot.
- plot add: Add a user to the editor list.
- plot remove: remove a user from the editor list.
- plot list: Lists who is an owner of this plot. You don't need to own it to use this.
- plot help: Displays descriptions of all commands.
- plot confirm (alias: /confirm): Confirm creation or destruction of a plot.
- plot cancel (alias: /cancel): Cancel creation or destruction of a plot.
