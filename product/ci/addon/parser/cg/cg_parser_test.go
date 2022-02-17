// Copyright 2021 Harness Inc. All rights reserved.
// Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
// that can be found in the licenses directory at the root of this repository, also available at
// https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.

package cg

import (
	"strings"
	"testing"

	"github.com/stretchr/testify/assert"
	"github.com/harness/harness-core/commons/go/lib/filesystem"
	"github.com/harness/harness-core/commons/go/lib/logs"
	"go.uber.org/zap"
)

func TestCallGraphParser_Parse(t *testing.T) {
	log, _ := logs.GetObservedLogger(zap.InfoLevel)
	fs := filesystem.NewOSFileSystem(log.Sugar())
	cgph := NewCallGraphParser(log.Sugar(), fs)
	dto, _ := cgph.Parse([]string{"testdata/cg.json"}, []string{})

	// Assert relations is as expected
	exp := map[int][]int{
		-776083018:  {-1648419296},
		1062598667:  {-1648419296},
		-2078257563: {1020759395},
		-1136127725: {1020759395},
		-849735784:  {1020759395},
		-1954679604: {-268233532},
		2139952358:  {-1648419296},
		330989721:   {-1648419296},
	}
	for _, v := range dto.TestRelations {
		assert.Equal(t, v.Tests, exp[v.Source])
	}

	// Assert the length of the Nodes parsed
	assert.Equal(t, len(dto.Nodes), 11)

	// Validate if a specific node exists in the parsed list
	sourceNode := Node{
		Package: "io.haness.exception",
		Method:  "<init>",
		ID:      2139952358,
		Params:  "java.lang.Sting,java.util.EnumSet",
		Class:   "InvalidAgumentsException",
		Type:    "source",
	}

	// Validate if a test node exists in the parsed list
	testNode := Node{
		Package:         "software.wings.sevice.intfc.signup",
		CallsReflection: true,
		Method:          "testValidateNameThowsInvalidAgumentsException",
		Params:          "void",
		Class:           "SignupSeviceTest",
		ID:              -1648419296,
		Type:            "test",
	}

	srcCnt := 0
	testCnt := 0
	for _, node := range dto.Nodes {
		if node == sourceNode {
			srcCnt += 1
		}
		if node == testNode {
			testCnt += 1
		}
	}
	assert.Equal(t, srcCnt, 1)
	assert.Equal(t, testCnt, 1)
}

func TestCallGraphParser_ParseShouldFail(t *testing.T) {
	log, _ := logs.GetObservedLogger(zap.InfoLevel)
	fs := filesystem.NewOSFileSystem(log.Sugar())
	cgph := NewCallGraphParser(log.Sugar(), fs)
	_, err := cgph.Parse([]string{"testdata/cg_invalid.json"}, []string{})

	assert.NotEqual(t, nil, err)
	assert.True(t, strings.Contains(err.Error(), "data unmarshalling to json failed for line"))
}

func TestCallGraphParser_ParseShouldFailNoFile(t *testing.T) {
	log, _ := logs.GetObservedLogger(zap.InfoLevel)
	fs := filesystem.NewOSFileSystem(log.Sugar())
	cgph := NewCallGraphParser(log.Sugar(), fs)
	_, err := cgph.Parse([]string{"testdata/cg_random.json"}, []string{})

	assert.NotEqual(t, nil, err)
	strings.Contains(err.Error(), "failed to open file")
	assert.True(t, strings.Contains(err.Error(), "failed to open file"))
}
