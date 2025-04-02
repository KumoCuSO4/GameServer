package main

import (
    "math/rand"
    "time"
    "github.com/bojand/ghz/runner"
	"github.com/bojand/ghz/printer"
	"log"
	"os"
)

// 生成随机 name 和 email
func generateRandomData() *RegisterReq {
    rand.Seed(time.Now().UnixNano())
    return &RegisterReq{
        Name:  randomString(6),  // 生成6位随机字符串
        Email: randomString(8) + "@test.com",
    }
}

func randomString(length int) string {
    letters := []rune("abcdefghijklmnopqrstuvwxyz")
    b := make([]rune, length)
    for i := range b {
        b[i] = letters[rand.Intn(len(letters))]
    }
    return string(b)
}

func main() {
    // 生成 1000 条随机数据
    var requests []*RegisterReq
    for i := 0; i < 1000000; i++ {
        requests = append(requests, generateRandomData())
    }

    // 执行压测
    report, err := runner.Run(
		"PlayerService.register",
		"172.19.240.91:8082",
		runner.WithProtoFile("./messages.proto", []string{}), 
		runner.WithTotalRequests(1000000),  
		runner.WithConcurrencySchedule(runner.ScheduleLine),
		runner.WithConcurrencyStep(1000),
		runner.WithConcurrencyStart(500),
		runner.WithConcurrencyEnd(10000),
		runner.WithData(requests),
		runner.WithInsecure(true),
	)
    if err != nil {
        panic(err)
    }

	file, err := os.Create("report.html")
	if err != nil {
		log.Fatal(err)
		return
	}
	rp := printer.ReportPrinter{
		Out:    file,
		Report: report,
	}
	// 指定输出格式
	_ = rp.Print("html")
}