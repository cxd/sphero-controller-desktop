require("ggplot2")
source("multiplot.R")
source("sphere.R")
data <- read.csv("csv/test1.csv", header=TRUE, sep=",")

p1 <- ggplot(data) + 
  geom_point(aes(x=x, y=y))

p2 <- ggplot(data) + 
  geom_point(aes(x=x, y=y), col="red")

multiplot(p1, p2, cols=2)

polar <- polarXY(data$x, data$y)

p3 <- ggplot(polar) +
  geom_line(aes(x=radius, y=theta, col=theta)) +
  geom_point(aes(x=radius, y=theta), col="red", alpha=0.1) +
  coord_polar(theta="y")


multiplot(p1, p2, p3, cols=2)
