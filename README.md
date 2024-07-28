**Custom Indicators I have created for MotiveWave using Java.**

These are custom indicators (or studies in MotiveWave) that I have created to supplement strategies I used for scalping (or intraday trading) ES futures.

I do not recommend or advice which indicators to use, this is for reference only.
Nor do I provide any trading or investment advice.
With that said, I actively use Price-Delta convergence for strength confirmation (or absence thereof). It does help recognize trend (or technically momentum) and complements OrderFlow and PriceAction strategies.

Remember, there is no magic indicator for successful trading.

Feel free to copy and use (MIT license)

Troubleshooting Notes
1. when exporting jar (build) do not include lib/** when the jars referenced are already in BIN. Doing so could result in MotiveWave error "Unable to register.... Missing StudyHeader Annotation"
2. Build using JDK 19 (or whatever was used for the motivewave sdk you are referencing)