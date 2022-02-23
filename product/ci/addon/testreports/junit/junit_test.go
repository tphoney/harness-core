// Copyright 2021 Harness Inc. All rights reserved.
// Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
// that can be found in the licenses directory at the root of this repository, also available at
// https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.

package junit

import (
	"context"
	"fmt"
	"io"
	"os"

	//"path/filepath"
	"testing"

	"github.com/stretchr/testify/assert"
	"github.com/harness/harness-core/product/ci/ti-service/types"
	"go.uber.org/zap"
)

var (
	prefix  = "junit_test_999/"
	report1 = "testdata/reportWithPassFail.xml"
	report2 = "testdata/reportWithSkipError.xml"
)

func getBaseDir() string {
	wd, _ := os.Getwd()
	fmt.Println("Working directory is: ", wd)
	return wd + prefix
}

// createNestedDir will create a nested directory relative to default temp directory
func createNestedDir(path string) error {
	absPath := getBaseDir() + path
	err := os.MkdirAll(absPath, 0777)
	if err != nil {
		return fmt.Errorf("could not create directory structure for testing: %s", err)
	}
	return nil
}

// removeBaseDir will clean up the temp directory
func removeBaseDir() error {
	err := os.RemoveAll(getBaseDir())
	if err != nil {
		return err
	}
	return nil
}

// copy file from src to relative dst in temp directory. Any existing file will be overwritten.
func copy(src, relDst string) error {
	dst := getBaseDir() + relDst
	in, err := os.Open(src)
	if err != nil {
		return err
	}
	defer in.Close()

	out, err := os.Create(dst)
	if err != nil {
		return err
	}
	defer out.Close()

	_, err = io.Copy(out, in)
	if err != nil {
		return err
	}
	return out.Close()
}

func expectedPassedTest() *types.TestCase {
	return &types.TestCase{
		Name:      "report1test1",
		ClassName: "report1test1class",
		SuiteName: "report1",
		Result: types.Result{
			Status: types.StatusPassed,
		},
		DurationMs: 123000,
		SystemOut:  "report1test1stdout",
		SystemErr:  "report1test1stderr",
	}
}

func expectedFailedTest() *types.TestCase {
	return &types.TestCase{
		Name:      "report1test2",
		ClassName: "report1test2class",
		SuiteName: "report1",
		Result: types.Result{
			Status:  types.StatusFailed,
			Message: "report1test2message",
			Type:    "report1test2type",
			Desc:    "report1test2description",
		},
		DurationMs: 11000,
		SystemOut:  "report1test2stdout",
		SystemErr:  "report1test2stderr",
	}
}

func expectedSkippedTest() *types.TestCase {
	return &types.TestCase{
		Name:      "report2test1",
		ClassName: "report2test1class",
		SuiteName: "report2",
		Result: types.Result{
			Status:  types.StatusSkipped,
			Message: "report2test1message",
			Desc:    "report2test1description",
		},
		DurationMs: 123000,
		SystemOut:  "report2test1stdout",
		SystemErr:  "report2test1stderr",
	}
}

func expectedErrorTest() *types.TestCase {
	return &types.TestCase{
		Name:      "report2test2",
		ClassName: "report2test2class",
		SuiteName: "report2",
		Result: types.Result{
			Status:  types.StatusError,
			Message: "report2test2message",
			Type:    "report2test2type",
			Desc:    "report2test2description",
		},
		DurationMs: 11000,
		SystemOut:  "report2test2stdout",
		SystemErr:  "report2test2stderr",
	}
}

func TestGetTests_All(t *testing.T) {
	err := createNestedDir("a/b/c/d")
	if err != nil {
		t.Fatal(err)
	}
	err = copy(report1, "a/b/report1.xml")
	if err != nil {
		t.Fatal(err)
	}
	err = copy(report2, "a/b/c/d/report2.xml")
	if err != nil {
		t.Fatal(err)
	}
	defer removeBaseDir()
	var paths []string
	paths = append(paths, getBaseDir()+"**/*.xml") // Regex to get both reports
	log := zap.NewExample().Sugar()
	j := New(paths, log)
	testc := j.GetTests(context.Background())
	var tests []*types.TestCase
	for tc := range testc {
		tests = append(tests, tc)
	}
	exp := []*types.TestCase{expectedPassedTest(), expectedErrorTest(), expectedFailedTest(), expectedSkippedTest()}
	assert.ElementsMatch(t, exp, tests)
	//assert.NotNil(t, nil)
}

func TestGetTests_All_MultiplePaths(t *testing.T) {
	err := createNestedDir("a/b/c/d")
	if err != nil {
		t.Fatal(err)
	}
	err = copy(report1, "a/b/report1.xml")
	if err != nil {
		t.Fatal(err)
	}
	err = copy(report2, "a/b/c/d/report2.xml")
	if err != nil {
		t.Fatal(err)
	}
	defer removeBaseDir()
	basePath := getBaseDir()
	var paths []string
	// Add multiple paths to get repeated filenames. Tests should still be unique (per filename)
	paths = append(paths, basePath+"a/*/*.xml") // Regex to get both reports
	paths = append(paths, basePath+"a/**/*.xml")
	paths = append(paths, basePath+"a/b/c/d/*.xml")
	log := zap.NewExample().Sugar()
	j := New(paths, log)
	testc := j.GetTests(context.Background())
	var tests []*types.TestCase
	for tc := range testc {
		tests = append(tests, tc)
	}
	exp := []*types.TestCase{expectedPassedTest(), expectedErrorTest(), expectedFailedTest(), expectedSkippedTest()}
	assert.ElementsMatch(t, exp, tests)
}

func TestGetTests_FirstRegex(t *testing.T) {
	err := createNestedDir("a/b/c/d")
	if err != nil {
		t.Fatal(err)
	}
	err = copy(report1, "a/b/report1.xml")
	if err != nil {
		t.Fatal(err)
	}
	err = copy(report2, "a/b/c/d/report2.xml")
	if err != nil {
		t.Fatal(err)
	}
	defer removeBaseDir()
	basePath := getBaseDir()
	var paths []string
	paths = append(paths, basePath+"a/b/*.xml") // Regex to get both reports
	log := zap.NewExample().Sugar()
	j := New(paths, log)
	testc := j.GetTests(context.Background())
	var tests []*types.TestCase
	for tc := range testc {
		tests = append(tests, tc)
	}
	exp := []*types.TestCase{expectedPassedTest(), expectedFailedTest()}
	assert.ElementsMatch(t, exp, tests)
}

func TestGetTests_SecondRegex(t *testing.T) {
	err := createNestedDir("a/b/c/d")
	if err != nil {
		t.Fatal(err)
	}
	err = copy(report1, "a/b/report1.xml")
	if err != nil {
		t.Fatal(err)
	}
	err = copy(report2, "a/b/c/d/report2.xml")
	if err != nil {
		t.Fatal(err)
	}
	defer removeBaseDir()
	basePath := getBaseDir()
	var paths []string
	paths = append(paths, basePath+"a/b/**/*2.xml") // Regex to get both reports
	log := zap.NewExample().Sugar()
	j := New(paths, log)
	testc := j.GetTests(context.Background())
	var tests []*types.TestCase
	for tc := range testc {
		tests = append(tests, tc)
	}
	exp := []*types.TestCase{expectedSkippedTest(), expectedErrorTest()}
	assert.ElementsMatch(t, exp, tests)
}

func TestGetTests_NoMatchingRegex(t *testing.T) {
	err := createNestedDir("a/b/c/d")
	if err != nil {
		t.Fatal(err)
	}
	err = copy(report1, "a/b/report1.xml")
	if err != nil {
		t.Fatal(err)
	}
	err = copy(report2, "a/b/c/d/report2.xml")
	if err != nil {
		t.Fatal(err)
	}
	defer removeBaseDir()
	basePath := getBaseDir()
	var paths []string
	paths = append(paths, basePath+"a/b/**/*3.xml") // Regex to get both reports
	log := zap.NewExample().Sugar()
	j := New(paths, log)
	testc := j.GetTests(context.Background())
	var tests []*types.TestCase
	for tc := range testc {
		tests = append(tests, tc)
	}
	exp := []*types.TestCase{}
	assert.ElementsMatch(t, exp, tests)
}
