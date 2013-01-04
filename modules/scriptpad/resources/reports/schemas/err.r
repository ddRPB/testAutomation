# This sample code returns the query data in tab-separated values format, which LabKey then
# renders as HTML. Replace this code with your R script. See the Help tab for more details.
datafile <- paste(labkey.url.base, "input_data.tsv", sep="");
labkey.data <- read.table(datafile, header=TRUE, sep="\t", quote="\"", comment.char="")
png(filename="${imgout:Blood Pressure/Single.jpg}");
plot(labkey.data$diastolicbloodpressure, labkey.data$systolicbloodpressure, 
main="Diastolic vs. Systolic Pressures: All Visits", 
ylab="Systolic (mm Hg)", xlab="Diastolic (mm Hg)", ylim =c(60, 200));
abline(lsfit(labkey.data$diastolicbloodpressure, labkey.data$systolicbloodpressure));
dev.off();
print unexpectedSymbol

