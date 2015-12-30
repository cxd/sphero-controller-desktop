require("ggplot2")
source("multiplot.R")
source("sphere.R")
data <- read.csv("csv/rand_data1.csv", header=TRUE, sep=",")

data2 <- read.csv("csv/binom_data1.csv", header=TRUE, sep=",")

p1 <- ggplot(data) + 
  geom_point(aes(x=x, y=y))

p2 <- ggplot(data2) + 
  geom_point(aes(x=x, y=y), col="red")

multiplot(p1, p2, cols=2)

polar <- polarXY(data$x, data$y)

p3 <- ggplot(polar) +
  geom_line(aes(x=radius, y=theta, col=theta)) +
  geom_point(aes(x=radius, y=theta), col="red", alpha=0.1) +
  coord_polar(theta="y")

polar2 <- polarXY(data2$x, data2$y)

p4 <- ggplot(polar2) +
  geom_line(aes(x=radius, y=theta, col=theta)) +
  geom_point(aes(x=radius, y=theta), col="red", alpha=0.1) +
  coord_polar(theta="y")

multiplot(p1, p2, p3, p4, cols=2)

